package com.bmtp.production.simulator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshSimulatorTests {

    @Test
    fun `simulator provisions nodes successfully`() {
        val simulator = MeshSimulator()
        simulator.provisionNodes(100)
        
        assertEquals(100, simulator.activeNodeCount())
        simulator.teardown()
    }

    @Test
    fun `simulator chaos mode terminates correct percentage of nodes`() {
        val simulator = MeshSimulator()
        simulator.provisionNodes(100)
        
        simulator.induceChaos(0.25) // Kill 25%
        
        assertEquals(75, simulator.activeNodeCount())
        simulator.teardown()
    }

    @Test
    fun `simulated transmission updates node metrics`() = runTest {
        // Ideal conditions (no loss, no latency)
        val conditions = NetworkConditions(packetLossProbability = 0.0, baseLatencyMs = 0L, latencyJitterMs = 0L)
        val simulator = MeshSimulator(conditions)
        
        simulator.provisionNodes(2)
        val nodeA = simulator.getNode("Node-0001")!!
        val nodeB = simulator.getNode("Node-0002")!!
        
        val payload = "Simulation Test".toByteArray()
        
        // Node A sends to Node B
        nodeA.send(nodeB.id, payload)
        
        // Use simulator to route the raw bytes (BmtpClient would normally do this via RoutingEngine)
        val delivered = simulator.simulateTransmission(nodeA.id, nodeB.id, payload)
        
        assertTrue(delivered)
        
        // Verify metrics
        assertEquals(1L, nodeA.packetsSent.get())
        assertEquals(1L, nodeB.packetsReceived.get())
        assertEquals(payload.size.toLong(), nodeA.bytesTransferred.get())
        
        simulator.teardown()
    }
}
