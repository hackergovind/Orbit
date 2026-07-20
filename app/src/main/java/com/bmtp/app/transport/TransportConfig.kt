package com.bmtp.app.transport

/**
 * Configuration parameters for the BMTP Reliable Transport Layer.
 */
data class TransportConfig(
    /** Initial timeout waiting for an ACK before retransmitting (ms). */
    val initialAckTimeoutMs: Long = 1000L,
    
    /** Maximum timeout waiting for an ACK (exponential backoff ceiling). */
    val maxAckTimeoutMs: Long = 8000L,
    
    /** Base multiplier for exponential backoff. */
    val backoffMultiplier: Float = 2.0f,
    
    /** Maximum number of times a packet will be retransmitted before failing. */
    val maxRetries: Int = 5,
    
    /** Maximum number of packets that can be held in the active memory queue. */
    val maxMemoryQueueSize: Int = 1000,
    
    /** Maximum number of packets that can be held in the offline persistent queue. */
    val maxOfflineQueueSize: Int = 5000,
    
    /** How long to wait before an out-of-order packet is dropped or requested again (ms). */
    val reorderWindowMs: Long = 5000L,
    
    /** Maximum size of the duplicate detection LRU cache. */
    val duplicateCacheSize: Int = 2000,
    
    /** Time after which a duplicate cache entry expires (ms). */
    val duplicateExpiryMs: Long = 60_000L,
    
    /** How often the timeout manager sweeps for expired packets/sessions (ms). */
    val timeoutSweepIntervalMs: Long = 1000L,
    
    /** Maximum duration a packet can stay in the offline queue before expiring (ms). Default: 24h */
    val offlinePacketExpiryMs: Long = 86_400_000L,
    
    /** File name for persisting the offline queue to disk. */
    val offlineQueueFileName: String = "bmtp_offline_queue.json",
    
    /** Minimum threshold of in-flight unacknowledged packets before triggering congestion control. */
    val congestionWindowInitialSize: Int = 10,
    
    /** The absolute maximum number of packets that can be in-flight to a single peer. */
    val congestionWindowMaxSize: Int = 50
)
