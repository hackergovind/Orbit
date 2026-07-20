package com.bmtp.app.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides structured concurrency wrappers that automatically yield CPU time
 * if the CpuManager reports high system load.
 */
@Singleton
class Scheduler @Inject constructor(
    private val metricsCollector: MetricsCollector
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Executes a computationally heavy block of work. 
     * If the CPU is overloaded, it will suspend and yield back to the dispatcher
     * to prevent starving the UI thread or audio processing.
     */
    suspend fun executeHeavyTask(block: suspend () -> Unit) {
        val cpuUsage = metricsCollector.metrics.value.cpuUsagePercent
        
        if (cpuUsage > 85f) {
            // High CPU load, voluntarily yield thread before starting
            yield()
            delay(10) // Give the system a brief breather
        }
        
        block()
    }

    /**
     * Executes work in the background on the IO dispatcher.
     */
    fun launchBackground(block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            block()
        }
    }
}
