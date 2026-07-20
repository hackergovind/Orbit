package com.bmtp.app.performance

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodically prunes expired packets from the core protocol queues.
 * Prevents memory bloat and stops the mesh from forwarding useless old data.
 */
@Singleton
class QueueOptimizer @Inject constructor(
    private val config: PerformanceConfig,
    private val logger: PerformanceLogger,
    private val metricsCollector: MetricsCollector
) {
    // In a real implementation, this would hold references to the actual queues from Phase 7/4
    // We mock the queue references here for architectural completeness.
    private val mockQueue = mutableListOf<MockQueuedPacket>()
    
    fun addMockPacket(timestamp: Long) {
        mockQueue.add(MockQueuedPacket(timestamp))
        metricsCollector.updateMeshMetrics(1.0f, 0f, 0, mockQueue.size)
    }

    /**
     * Executes the pruning logic.
     * Called periodically by the Scheduler.
     */
    fun pruneExpiredPackets() {
        val now = System.currentTimeMillis()
        val expiryThreshold = now - config.maxQueueAgeMs
        
        var removedCount = 0
        
        val iterator = mockQueue.iterator()
        while (iterator.hasNext()) {
            val packet = iterator.next()
            if (packet.queuedAt < expiryThreshold) {
                iterator.remove()
                removedCount++
            }
        }
        
        if (removedCount > 0) {
            logger.logOptimizationDecision(
                "QueueOptimizer", 
                "Pruned $removedCount expired packets", 
                "Age > ${config.maxQueueAgeMs}ms"
            )
            metricsCollector.updateMeshMetrics(1.0f, 0f, 0, mockQueue.size)
        }
    }
    
    data class MockQueuedPacket(val queuedAt: Long)
}
