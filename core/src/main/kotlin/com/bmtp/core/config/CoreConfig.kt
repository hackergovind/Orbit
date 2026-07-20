package com.bmtp.core.config

/**
 * Universal configuration for the BMTP Core framework.
 * This can be loaded from YAML, JSON, or Environment Variables depending on the platform.
 */
data class CoreConfig(
    val nodeId: String,
    
    // Routing Configuration
    val maxHops: Int = 7,
    val routeTimeoutMs: Long = 60000L,
    
    // Security Configuration
    val enableEncryption: Boolean = true,
    val allowUnauthenticatedNodes: Boolean = false,
    
    // Optimization
    val enableCompression: Boolean = true,
    val packetBatchingMs: Long = 50L
) {
    init {
        require(nodeId.isNotBlank()) { "Node ID must not be blank" }
        require(maxHops in 1..15) { "Max Hops must be between 1 and 15" }
    }
}
