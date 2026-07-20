package com.bmtp.app.performance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ScanParameters(
    val scanIntervalMs: Long,
    val isAggressive: Boolean
)

/**
 * Dynamically adjusts BLE scanning parameters to save battery
 * while maintaining mesh connectivity.
 */
@Singleton
class ScanOptimizer @Inject constructor(
    private val config: PerformanceConfig,
    private val metricsCollector: MetricsCollector,
    private val logger: PerformanceLogger
) {
    private val _scanParameters = MutableStateFlow(ScanParameters(config.baseScanIntervalMs, true))
    val scanParameters: StateFlow<ScanParameters> = _scanParameters.asStateFlow()

    /**
     * Evaluates current system metrics and adjusts scanning behavior.
     * Called periodically by the Scheduler.
     */
    fun evaluateScanning() {
        val metrics = metricsCollector.metrics.value
        val currentParams = _scanParameters.value
        
        var newInterval = config.baseScanIntervalMs
        var newAggressive = true
        var reason = "Normal"

        if (metrics.isBatteryCritical) {
            // Critical battery: Scan very rarely, only rely on existing connections
            newInterval = config.relaxedScanIntervalMs * 2
            newAggressive = false
            reason = "Battery Critical"
        } else if (metrics.batteryPercent < config.lowBatteryThresholdPercent && !metrics.isCharging) {
            // Low battery: Relax scanning
            newInterval = config.relaxedScanIntervalMs
            newAggressive = false
            reason = "Battery Low"
        } else if (metrics.activeConnections >= 5) { // Assuming 5 is a healthy neighbor count
            // Healthy mesh: No need to aggressively scan for more neighbors
            newInterval = config.relaxedScanIntervalMs
            newAggressive = false
            reason = "Sufficient Neighbors (${metrics.activeConnections})"
        }

        if (newInterval != currentParams.scanIntervalMs || newAggressive != currentParams.isAggressive) {
            _scanParameters.value = ScanParameters(newInterval, newAggressive)
            logger.logOptimizationDecision(
                "ScanOptimizer", 
                "Adjusted Scan Interval to ${newInterval}ms (Aggressive: $newAggressive)", 
                reason
            )
        }
    }
}
