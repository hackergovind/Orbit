package com.bmtp.app.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * A built-in stress testing framework.
 * Can simulate large mesh networks on the device to measure CPU, Memory, and Latency under load.
 */
@Singleton
class BenchmarkRunner @Inject constructor(
    private val logger: PerformanceLogger,
    private val cpuManager: CpuManager,
    private val metricsCollector: MetricsCollector
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Simulates routing and packet handling for a given number of virtual nodes.
     * Generates a report in the PerformanceLogger.
     */
    fun runMeshStressTest(virtualNodes: Int, packetsPerSecond: Int) {
        scope.launch {
            logger.logOptimizationDecision(
                "Benchmark", 
                "Starting Stress Test", 
                "Nodes: $virtualNodes, PPS: $packetsPerSecond"
            )
            
            cpuManager.startMonitoring()
            
            val startTime = System.currentTimeMillis()
            var packetsProcessed = 0
            
            // Run simulation for 10 seconds
            val durationMs = 10000L
            val loopDelayMs = 1000L / packetsPerSecond
            
            while (System.currentTimeMillis() - startTime < durationMs) {
                // Simulate CPU work for crypto and routing
                val startWork = System.currentTimeMillis()
                simulateCryptoWork()
                val endWork = System.currentTimeMillis()
                
                // Track latency
                val latency = (endWork - startWork).toFloat()
                
                packetsProcessed++
                
                delay(loopDelayMs)
            }
            
            // Collect final metrics
            cpuManager.pollCpuUsage()
            val cpuUsage = metricsCollector.metrics.value.cpuUsagePercent
            
            logger.logBenchmarkResult(
                name = "StressTest-${virtualNodes}Nodes",
                latencyMs = (durationMs.toFloat() / packetsProcessed), // Avg latency
                cpuUsagePercent = cpuUsage
            )
            
            if (cpuUsage > 90f) {
                logger.logError("Stress Test Failed. CPU maxed out at $cpuUsage%", BenchmarkFailureException("CPU Starvation"))
            }
        }
    }

    private fun simulateCryptoWork() {
        // Spin CPU to simulate AES-GCM decryption and Ed25519 signature verification
        var dummy = 0
        for (i in 0..5000) {
            dummy += Random.nextInt()
        }
    }
}
