package com.bmtp.production.benchmark

import com.bmtp.production.simulator.MeshSimulator
import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis

/**
 * Orchestrates large-scale throughput and latency benchmarks over the MeshSimulator.
 * Generates automated reports.
 */
class BenchmarkRunner(private val simulator: MeshSimulator) {

    private val metrics = MetricsCollector()

    /**
     * Executes a throughput benchmark by blasting the network with data
     * and calculating delivery ratios.
     */
    suspend fun runThroughputTest(nodeCount: Int, packetsPerNode: Int): BenchmarkReport {
        println("Starting Benchmark: $nodeCount Nodes, $packetsPerNode Packets/Node")
        
        simulator.provisionNodes(nodeCount)
        
        val allNodes = (1..nodeCount).map { String.format("Node-%04d", it) }
        val payload = ByteArray(256) { 0x42 } // 256 bytes of dummy data
        
        val timeTakenMs = measureTimeMillis {
            for (senderId in allNodes) {
                val sender = simulator.getNode(senderId)!!
                // Each node sends packets to the next node in the list
                val targetIndex = (allNodes.indexOf(senderId) + 1) % allNodes.size
                val targetId = allNodes[targetIndex]
                
                for (i in 0 until packetsPerNode) {
                    val start = System.currentTimeMillis()
                    val delivered = simulator.simulateTransmission(senderId, targetId, payload)
                    val end = System.currentTimeMillis()
                    
                    if (delivered) {
                        metrics.recordDelivery(payload.size)
                        metrics.recordLatency(end - start)
                    } else {
                        metrics.recordDrop()
                    }
                }
            }
        }
        
        simulator.teardown()
        
        return BenchmarkReport(
            testName = "Throughput-Nodes$nodeCount-Packets$packetsPerNode",
            deliveryRatio = metrics.getDeliveryRatio(),
            averageLatencyMs = metrics.getAverageLatency(),
            totalTimeMs = timeTakenMs,
            totalPacketsDelivered = metrics.getTotalDelivered()
        )
    }
}

data class BenchmarkReport(
    val testName: String,
    val deliveryRatio: Double,
    val averageLatencyMs: Double,
    val totalTimeMs: Long,
    val totalPacketsDelivered: Long
) {
    fun printReport() {
        println("=== BENCHMARK REPORT: $testName ===")
        println("Total Time: ${totalTimeMs}ms")
        println("Packets Delivered: $totalPacketsDelivered")
        println("Delivery Ratio: ${"%.2f".format(deliveryRatio * 100)}%")
        println("Average Latency: ${"%.2f".format(averageLatencyMs)}ms")
        println("=========================================")
    }
}
