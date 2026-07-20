package com.bmtp.app.performance

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates route health metrics from the Phase 5 AODV routing table.
 * De-prioritizes routes that are unstable or traverse low-battery nodes.
 */
@Singleton
class RouteOptimizer @Inject constructor(
    private val logger: PerformanceLogger,
    private val cacheManager: CacheManager
) {
    // In a real implementation, this would interface with AODV Routing Table
    private val mockRoutingTable = mutableMapOf<String, MockRoute>()

    fun addMockRoute(destination: String, hops: Int, pdr: Float, latencyMs: Float) {
        mockRoutingTable[destination] = MockRoute(hops, pdr, latencyMs)
    }

    /**
     * Periodically reviews the routing table.
     * Routes with terrible packet delivery ratio (PDR) or massive latency are evicted.
     */
    fun optimizeRoutes() {
        val minAcceptablePdr = 0.50f // 50% delivery ratio
        val maxAcceptableLatency = 5000f // 5 seconds
        
        var evictedCount = 0
        
        val iterator = mockRoutingTable.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val route = entry.value
            
            if (route.packetDeliveryRatio < minAcceptablePdr || route.latencyMs > maxAcceptableLatency) {
                val reason = if (route.packetDeliveryRatio < minAcceptablePdr) "Low PDR (${route.packetDeliveryRatio})" 
                             else "High Latency (${route.latencyMs}ms)"
                             
                logger.logOptimizationDecision("RouteOptimizer", "Evicting route to ${entry.key}", reason)
                
                // Evict from CacheManager as well
                cacheManager.routeCache.remove(entry.key)
                
                iterator.remove()
                evictedCount++
            }
        }
    }
    
    data class MockRoute(
        val hopCount: Int,
        val packetDeliveryRatio: Float,
        val latencyMs: Float
    )
}
