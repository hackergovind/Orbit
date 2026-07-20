package com.bmtp.app.routing

/**
 * Configuration parameters for the BMTP AODV Routing Engine.
 */
data class RoutingConfig(
    /** Maximum allowed hop count before a route or request is deemed unreachable. */
    val maxHopCount: UByte = 15u,
    
    /** The default lifetime for a newly created or refreshed route in milliseconds. */
    val routeLifetimeMs: Long = 300_000L, // 5 minutes
    
    /** How often the RouteExpiryManager sweeps the routing table for stale routes. */
    val expirySweepIntervalMs: Long = 10_000L,
    
    /** Maximum number of active routes to hold in memory. */
    val maxRoutes: Int = 1000,
    
    /** Maximum number of recent routes to cache for ultra-fast lookup. */
    val cacheSize: Int = 100,
    
    /** Timeout in milliseconds to wait for a Route Reply (RREP) before declaring discovery failed. */
    val discoveryTimeoutMs: Long = 5000L,
    
    /** Number of times to retry Route Discovery (RREQ) before giving up. */
    val discoveryRetryCount: Int = 3,
    
    /** Time in milliseconds before attempting a local route repair upon link failure. */
    val repairTimeoutMs: Long = 2000L,
    
    /** Minimum acceptable RSSI to consider a route 'healthy'. */
    val rssiThreshold: Int = -85,
    
    /** Maximum acceptable latency (ms) for a route to be preferred. */
    val latencyThresholdMs: Long = 1000L,
    
    /** Interval in milliseconds to broadcast HELLO packets to detect neighbors. */
    val helloIntervalMs: Long = 10_000L
)
