package com.bmtp.app.routing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates a 'score' for a route based on hop count, RSSI, and latency.
 * Higher scores represent better routes.
 */
@Singleton
class RouteMetrics @Inject constructor(
    private val config: RoutingConfig
) {
    /**
     * Scores a potential route.
     * 
     * @param hopCount The number of hops to the destination.
     * @param rssi The RSSI of the next hop (-127 to 0).
     * @param latencyMs The latency in milliseconds (if known, else 0).
     * @return An integer score (higher is better).
     */
    fun calculateScore(hopCount: UByte, rssi: Int, latencyMs: Long): Int {
        var score = 10000
        
        // Penalize heavily for each hop
        score -= (hopCount.toInt() * 500)
        
        // Penalize for poor RSSI (values lower than threshold)
        if (rssi < config.rssiThreshold) {
            score -= ((config.rssiThreshold - rssi) * 10)
        }
        
        // Penalize for high latency
        if (latencyMs > 0) {
            score -= (latencyMs / 10).toInt()
        }
        
        return score.coerceAtLeast(0)
    }
}
