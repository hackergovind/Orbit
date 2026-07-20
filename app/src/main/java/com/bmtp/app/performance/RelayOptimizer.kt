package com.bmtp.app.performance

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adaptive Relay Selection logic.
 * Decides whether this specific node should participate in routing third-party traffic.
 */
@Singleton
class RelayOptimizer @Inject constructor(
    private val config: PerformanceConfig,
    private val metricsCollector: MetricsCollector,
    private val logger: PerformanceLogger
) {
    /**
     * Calculates a Relay Score from 0.0 to 1.0.
     * 0.0 means this node absolutely refuses to route traffic for others.
     * 1.0 means this node is a prime candidate for routing (plugged in, fast CPU, empty queues).
     * 
     * Called periodically by the Scheduler.
     */
    fun evaluateRelayCapacity(): Float {
        val metrics = metricsCollector.metrics.value
        
        // 1. Hard Constraints
        if (metrics.isBatteryCritical) {
            logger.logOptimizationDecision("RelayOptimizer", "Score = 0.0", "Battery is critical")
            metricsCollector.updateRelayScore(0.0f)
            return 0.0f
        }
        if (metrics.isMemoryPressureHigh) {
            logger.logOptimizationDecision("RelayOptimizer", "Score = 0.0", "High Memory Pressure")
            metricsCollector.updateRelayScore(0.0f)
            return 0.0f
        }

        // 2. Soft Scoring
        var score = 1.0f

        // Battery Penalty (if not charging)
        if (!metrics.isCharging) {
            // E.g., at 30% battery, penalty is 0.7. Score drops to 0.3
            val batteryPenalty = 1.0f - metrics.batteryPercent
            score -= (batteryPenalty * 0.5f) // Max 50% penalty for battery
        }

        // CPU Penalty
        if (metrics.cpuUsagePercent > 50f) {
            val cpuPenalty = (metrics.cpuUsagePercent - 50f) / 100f
            score -= cpuPenalty
        }

        // Queue Penalty (Heavy load)
        if (metrics.queuedPacketsCount > 100) {
            score -= 0.2f
        }

        // Neighbor Bonus (A well-connected node is a better relay)
        if (metrics.activeConnections > 3) {
            score += 0.1f
        }

        // Clamp between 0.0 and 1.0
        val finalScore = score.coerceIn(0.0f, 1.0f)
        
        metricsCollector.updateRelayScore(finalScore)
        return finalScore
    }
}
