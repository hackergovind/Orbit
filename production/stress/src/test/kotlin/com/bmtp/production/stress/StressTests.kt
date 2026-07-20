package com.bmtp.production.stress

import com.bmtp.production.benchmark.BenchmarkRunner
import com.bmtp.production.simulator.MeshSimulator
import com.bmtp.production.simulator.NetworkConditions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class StressTests {

    /**
     * Warning: This test provisions 1000 nodes and sends 10000 packets.
     * It is designed to expose memory leaks and thread exhaustion in the BmtpClient.
     */
    @Test
    fun `stress test - 1000 nodes massive throughput`() = runTest(timeout = 5.minutes) {
        // High latency jitter to ensure packets queue up and test backpressure
        val conditions = NetworkConditions(
            packetLossProbability = 0.05, // 5% loss
            baseLatencyMs = 50L,
            latencyJitterMs = 150L
        )
        
        val simulator = MeshSimulator(conditions)
        val runner = BenchmarkRunner(simulator)
        
        // 1000 nodes sending 10 packets each = 10,000 packets circulating simultaneously
        val report = runner.runThroughputTest(nodeCount = 1000, packetsPerNode = 10)
        
        report.printReport()
        
        // Ensure at least 85% delivery under 5% physical loss and massive congestion
        assertTrue("Delivery ratio too low under stress: ${report.deliveryRatio}", report.deliveryRatio > 0.85)
    }
}
