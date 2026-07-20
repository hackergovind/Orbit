package com.bmtp.production.benchmark

import com.bmtp.production.simulator.MeshSimulator
import com.bmtp.production.simulator.NetworkConditions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkRunnerTests {

    @Test
    fun `benchmark runner executes successfully under ideal conditions`() = runTest {
        val simulator = MeshSimulator(NetworkConditions(packetLossProbability = 0.0, baseLatencyMs = 1L, latencyJitterMs = 0L))
        val runner = BenchmarkRunner(simulator)
        
        // Fast test: 10 nodes, 5 packets each
        val report = runner.runThroughputTest(10, 5)
        
        report.printReport()
        
        // Under 0% packet loss config, delivery should be 100%
        assertTrue("Delivery ratio should be 1.0", report.deliveryRatio == 1.0)
        assertTrue("Total packets delivered should be 50", report.totalPacketsDelivered == 50L)
    }

    @Test
    fun `benchmark runner handles severe packet loss`() = runTest {
        val simulator = MeshSimulator(NetworkConditions(packetLossProbability = 0.5, baseLatencyMs = 1L, latencyJitterMs = 0L))
        val runner = BenchmarkRunner(simulator)
        
        val report = runner.runThroughputTest(10, 10)
        
        report.printReport()
        
        // With 50% loss, delivery should be around 50%
        assertTrue("Delivery ratio should be > 0.0", report.deliveryRatio > 0.0)
        assertTrue("Delivery ratio should be < 1.0", report.deliveryRatio < 1.0)
    }
}
