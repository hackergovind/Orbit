package com.bmtp.app.performance

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodically evaluates active connections.
 * Disconnects idle or weak connections if we have enough healthy ones.
 */
@Singleton
class ConnectionOptimizer @Inject constructor(
    private val logger: PerformanceLogger,
    private val metricsCollector: MetricsCollector
) {
    // In a real implementation, this would interface with the Phase 2 BLE Transport
    private val activeConnections = mutableMapOf<String, MockConnectionState>()

    fun addMockConnection(id: String, rssi: Int, lastActiveAt: Long) {
        activeConnections[id] = MockConnectionState(rssi, lastActiveAt)
        metricsCollector.updateMeshMetrics(1.0f, 0f, activeConnections.size, 0)
    }

    /**
     * Executes the connection pruning logic.
     * Called periodically by the Scheduler.
     */
    fun optimizeConnections() {
        val now = System.currentTimeMillis()
        val idleThreshold = now - (5 * 60 * 1000L) // 5 minutes idle
        val weakRssiThreshold = -85
        
        var prunedCount = 0
        
        // Only prune if we have a healthy number of connections
        if (activeConnections.size > 3) {
            val iterator = activeConnections.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val state = entry.value
                
                val isIdle = state.lastActiveAt < idleThreshold
                val isWeak = state.rssi < weakRssiThreshold
                
                if (isIdle || isWeak) {
                    val reason = if (isIdle) "Idle > 5m" else "Weak RSSI (${state.rssi})"
                    logger.logOptimizationDecision("ConnectionOptimizer", "Disconnecting ${entry.key}", reason)
                    iterator.remove()
                    prunedCount++
                    
                    // Don't prune too many at once to avoid network partitioning
                    if (activeConnections.size <= 3) break
                }
            }
        }
        
        if (prunedCount > 0) {
            metricsCollector.updateMeshMetrics(1.0f, 0f, activeConnections.size, 0)
        }
    }
    
    data class MockConnectionState(val rssi: Int, val lastActiveAt: Long)
}
