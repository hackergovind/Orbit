package com.bmtp.production.simulator

/**
 * Defines the environmental physics for the Mesh Simulator.
 * Allows simulating real-world network degradation, packet loss, and latency jitter.
 */
data class NetworkConditions(
    /**
     * Probability (0.0 to 1.0) that a packet is dropped completely during transit.
     */
    val packetLossProbability: Double = 0.0,
    
    /**
     * Base latency in milliseconds for a packet to travel between two adjacent nodes.
     */
    val baseLatencyMs: Long = 10L,
    
    /**
     * Maximum random jitter added to the base latency.
     */
    val latencyJitterMs: Long = 5L,
    
    /**
     * Maximum number of connections a single node can sustain simultaneously.
     * Simulates Bluetooth LE physical connection limits (usually 7-15).
     */
    val maxConnectionsPerNode: Int = 15,
    
    /**
     * Artificial battery drain multiplier. 
     */
    val batteryDrainMultiplier: Double = 1.0
) {
    init {
        require(packetLossProbability in 0.0..1.0) { "Packet loss must be between 0.0 and 1.0" }
    }
}
