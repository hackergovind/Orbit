package com.bmtp.app.performance

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors memory usage and hooks into Android's low memory callbacks.
 * Coordinates with CacheManager to drop non-essential data when pressure is high.
 */
@Singleton
class BmtpMemoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metricsCollector: MetricsCollector,
    private val cacheManager: CacheManager, // Will be implemented next
    private val logger: PerformanceLogger
) : ComponentCallbacks2 {

    private var isRegistered = false

    fun startMonitoring() {
        if (!isRegistered) {
            context.registerComponentCallbacks(this)
            updateMemoryMetrics(false)
            isRegistered = true
        }
    }

    fun stopMonitoring() {
        if (isRegistered) {
            context.unregisterComponentCallbacks(this)
            isRegistered = false
        }
    }

    override fun onTrimMemory(level: Int) {
        val isPressureHigh = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
        
        if (isPressureHigh) {
            logger.logOptimizationDecision("MemoryManager", "Evicting caches due to high memory pressure (Level $level)", "onTrimMemory")
            cacheManager.evictAll()
            updateMemoryMetrics(true)
            
            // In extreme cases, throw an exception to disrupt large operations
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                throw MemoryPressureException("Critical memory pressure hit.")
            }
        } else {
            // Mild pressure, just trim unused pools
            cacheManager.trimToSize()
            updateMemoryMetrics(false)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No-op for memory tracking
    }

    override fun onLowMemory() {
        // Legacy callback, essentially TRIM_MEMORY_COMPLETE
        logger.logOptimizationDecision("MemoryManager", "Evicting ALL caches", "onLowMemory")
        cacheManager.evictAll()
        updateMemoryMetrics(true)
    }
    
    private fun updateMemoryMetrics(isHighPressure: Boolean) {
        val runtime = Runtime.getRuntime()
        val usedMemBytes = runtime.totalMemory() - runtime.freeMemory()
        val usedMemMb = usedMemBytes / (1024 * 1024)
        
        metricsCollector.updateMemory(usedMemMb, isHighPressure)
    }
    
    /**
     * Poll this periodically from the PerformanceManager to track MB usage.
     */
    fun pollMemory() {
        val currentMetrics = metricsCollector.metrics.value
        updateMemoryMetrics(currentMetrics.isMemoryPressureHigh)
    }
}
