package com.bmtp.app.performance

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized logger for the Performance & Scalability Engine.
 * Designed to minimize string interpolation overhead in tight optimization loops.
 */
@Singleton
class PerformanceLogger @Inject constructor() {
    private val subtag = "BMTP-Perf"

    fun logBatteryModeChanged(isCritical: Boolean, isCharging: Boolean) {
        val mode = if (isCritical && !isCharging) "CRITICAL_LOW_POWER" else if (isCharging) "CHARGING" else "NORMAL"
        LogUtils.i(subtag, "Battery Mode Transition -> $mode")
    }

    fun logOptimizationDecision(component: String, decision: String, reason: String) {
        LogUtils.d(subtag, "OPT [$component]: $decision | Reason: $reason")
    }

    fun logMemoryCleanup(bytesFreed: Long, cacheItemsEvicted: Int) {
        LogUtils.i(subtag, "Memory Cleanup | Freed ~${bytesFreed / 1024} KB | Evicted: $cacheItemsEvicted items")
    }

    fun logBenchmarkResult(name: String, latencyMs: Float, cpuUsagePercent: Float) {
        LogUtils.i(subtag, "BENCHMARK [$name] -> Latency: ${latencyMs}ms | CPU: $cpuUsagePercent%")
    }

    fun logAnomaly(component: String, description: String) {
        LogUtils.w(subtag, "ANOMALY [$component]: $description")
    }

    fun logError(message: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, message, throwable)
    }

    // High-frequency trace logging for metrics, completely disabled in production builds
    fun traceMetric(metricName: String, value: Number) {
        // Uncomment for deep debugging, but leave commented out for performance
        // LogUtils.v(subtag, "$metricName: $value")
    }
}
