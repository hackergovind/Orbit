package com.bmtp.production.simulator

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * The core orchestration engine for large-scale mesh simulation.
 * Capable of spinning up thousands of VirtualNodes and dictating the network conditions between them.
 */
class MeshSimulator(
    val conditions: NetworkConditions = NetworkConditions()
) {
    private val nodes = ConcurrentHashMap<String, VirtualNode>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Provisions and boots a cluster of virtual nodes.
     */
    fun provisionNodes(count: Int) {
        println("Provisioning $count virtual nodes...")
        for (i in 1..count) {
            val nodeId = "Node-${String.format("%04d", i)}"
            val node = VirtualNode(nodeId)
            nodes[nodeId] = node
            node.boot()
        }
        println("Provisioning complete.")
    }

    /**
     * Simulates a physical transmission from one node to another.
     * Applies configured latency, jitter, and packet loss.
     */
    suspend fun simulateTransmission(fromId: String, toId: String, payload: ByteArray): Boolean {
        val destination = nodes[toId] ?: return false

        // 1. Packet Loss Simulation
        if (conditions.packetLossProbability > 0.0) {
            if (Random.nextDouble() < conditions.packetLossProbability) {
                // Packet dropped by the ether
                return false
            }
        }

        // 2. Latency Simulation
        val jitter = if (conditions.latencyJitterMs > 0) Random.nextLong(conditions.latencyJitterMs) else 0L
        val totalLatency = conditions.baseLatencyMs + jitter
        
        delay(totalLatency)

        // 3. Delivery
        destination.transport.receivePacket(fromId, payload)
        destination.recordReceive(payload.size)
        return true
    }

    /**
     * Instantly kills a random percentage of nodes to test routing partition recovery.
     */
    fun induceChaos(killPercentage: Double) {
        require(killPercentage in 0.0..1.0)
        val killCount = (nodes.size * killPercentage).toInt()
        
        val nodesToKill = nodes.keys.toList().shuffled().take(killCount)
        nodesToKill.forEach { id ->
            nodes[id]?.shutdown()
            nodes.remove(id)
        }
        println("CHAOS: Terminated $killCount nodes.")
    }

    /**
     * Gracefully tears down the entire simulation.
     */
    fun teardown() {
        nodes.values.forEach { it.shutdown() }
        nodes.clear()
        scope.cancel()
    }
    
    /**
     * Retrieves an active node by ID.
     */
    fun getNode(id: String): VirtualNode? = nodes[id]
    
    /**
     * Returns the current number of active nodes.
     */
    fun activeNodeCount(): Int = nodes.size
}
