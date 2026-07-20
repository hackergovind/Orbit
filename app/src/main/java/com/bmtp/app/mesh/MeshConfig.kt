package com.bmtp.app.mesh

/**
 * Configuration parameters for the BMTP Mesh Forwarding Engine.
 */
data class MeshConfig(
    /** Maximum allowed hop count before a packet is dropped. */
    val maxHopCount: UByte = 15u,
    
    /** Maximum number of packets that can be held in the store-and-forward queue. */
    val forwardQueueSize: Int = 500,
    
    /** Time in milliseconds before a disconnected neighbor is forgotten. */
    val neighborTimeoutMs: Long = 30_000L,
    
    /** How long a packet can stay in the forward queue before expiring. */
    val packetExpirationMs: Long = 60_000L,
    
    /** Delay in milliseconds before retrying to forward a queued packet. */
    val retryDelayMs: Long = 5000L,
    
    /** Artificial delay added before relaying a packet to prevent broadcast storms. */
    val relayDelayMs: Long = 50L
)
