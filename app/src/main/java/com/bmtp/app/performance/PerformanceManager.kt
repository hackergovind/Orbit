package com.bmtp.app.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Top-level Orchestrator for Phase 11.
 * Ties together CPU, Battery, Memory, and Mesh optimization engines.
 */
@Singleton
class PerformanceManager @Inject constructor(
    private val config: PerformanceConfig,
    private val batteryManager: BmtpBatteryManager,
    private val memoryManager: BmtpMemoryManager,
    private val cpuManager: CpuManager,
    private val scanOptimizer: ScanOptimizer,
    private val connectionOptimizer: ConnectionOptimizer,
    private val relayOptimizer: RelayOptimizer,
    private val routeOptimizer: RouteOptimizer,
    private val queueOptimizer: QueueOptimizer,
    val cacheManager: CacheManager, // Exposed for other modules
    val metricsCollector: MetricsCollector,
    private val logger: PerformanceLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var optimizationJob: Job? = null

    /**
     * Starts background optimization loops and registers OS listeners.
     */
    fun startOptimizationEngine() {
        if (optimizationJob != null) return

        logger.logOptimizationDecision("PerformanceManager", "Starting Engine", "App Startup")
        
        batteryManager.startMonitoring()
        memoryManager.startMonitoring()
        cpuManager.startMonitoring()

        optimizationJob = scope.launch {
            while (isActive) {
                delay(config.metricsSamplingIntervalMs)
                
                try {
                    runOptimizationCycle()
                } catch (e: Exception) {
                    logger.logError("Optimization cycle failed", e)
                }
            }
        }
    }

    /**
     * Stops the engine. Typically called when app goes into deep background or user stops the service.
     */
    fun stopOptimizationEngine() {
        logger.logOptimizationDecision("PerformanceManager", "Stopping Engine", "App Teardown")
        
        optimizationJob?.cancel()
        optimizationJob = null
        
        batteryManager.stopMonitoring()
        memoryManager.stopMonitoring()
    }

    private fun runOptimizationCycle() {
        // 1. Poll OS Metrics
        cpuManager.pollCpuUsage()
        memoryManager.pollMemory()

        // 2. Mesh Optimization
        scanOptimizer.evaluateScanning()
        connectionOptimizer.optimizeConnections()
        
        // 3. Routing & Relaying
        relayOptimizer.evaluateRelayCapacity()
        routeOptimizer.optimizeRoutes()
        
        // 4. Housekeeping
        queueOptimizer.pruneExpiredPackets()
    }
}
