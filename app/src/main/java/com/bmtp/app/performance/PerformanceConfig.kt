package com.bmtp.app.performance

/**
 * Global configuration parameters for the BMTP Performance Engine.
 */
data class PerformanceConfig(
    /** Threshold below which battery is considered critical. Node will stop relaying. */
    val criticalBatteryThresholdPercent: Float = 0.15f,
    
    /** Threshold below which battery is considered low. Node will optimize scanning. */
    val lowBatteryThresholdPercent: Float = 0.30f,
    
    /** Max size of LRU caches before eviction occurs. */
    val maxCacheEntries: Int = 10000,
    
    /** TTL for cached packets in milliseconds (e.g., 5 minutes). */
    val packetCacheTtlMs: Long = 5 * 60 * 1000L,
    
    /** Max objects to keep in a reusable ObjectPool (e.g., empty byte arrays). */
    val maxObjectPoolSize: Int = 50,
    
    /** Base scan interval when battery is healthy (in ms). */
    val baseScanIntervalMs: Long = 5000L,
    
    /** Scan interval when battery is low or many neighbors exist (in ms). */
    val relaxedScanIntervalMs: Long = 15000L,
    
    /** Max age of a queued packet before it is pruned by QueueOptimizer (in ms). */
    val maxQueueAgeMs: Long = 24 * 60 * 60 * 1000L, // 24 hours
    
    /** Rate at which metrics are sampled and aggregated (in ms). */
    val metricsSamplingIntervalMs: Long = 10000L
)
