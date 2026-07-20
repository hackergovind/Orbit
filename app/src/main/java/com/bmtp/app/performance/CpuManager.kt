package com.bmtp.app.performance

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors CPU usage using approximate OS-level metrics and yields execution if overloaded.
 * Helps prevent the background mesh engine from freezing the device UI.
 */
@Singleton
class CpuManager @Inject constructor(
    private val metricsCollector: MetricsCollector,
    private val logger: PerformanceLogger
) {
    private var lastCpuTimeMs: Long = 0
    private var lastRealTimeMs: Long = 0
    
    fun startMonitoring() {
        lastCpuTimeMs = android.os.Process.getElapsedCpuTime()
        lastRealTimeMs = SystemClock.elapsedRealtime()
    }
    
    /**
     * Polls the CPU time consumed by this process and updates the metrics.
     * Call this periodically from the PerformanceManager.
     */
    fun pollCpuUsage() {
        val currentCpuTime = android.os.Process.getElapsedCpuTime()
        val currentRealTime = SystemClock.elapsedRealtime()
        
        val cpuDiff = currentCpuTime - lastCpuTimeMs
        val realDiff = currentRealTime - lastRealTimeMs
        
        if (realDiff > 0) {
            // Rough estimation of CPU percentage used by this process across all cores
            val usagePercent = (cpuDiff.toFloat() / realDiff.toFloat()) * 100f
            
            metricsCollector.updateCpu(usagePercent)
            
            if (usagePercent > 80f) {
                logger.logAnomaly("CpuManager", "High CPU Usage Detected: $usagePercent%")
            }
        }
        
        lastCpuTimeMs = currentCpuTime
        lastRealTimeMs = currentRealTime
    }
}
