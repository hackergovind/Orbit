package com.bmtp.transport

/**
 * Common configuration for any BMTP Transport implementation.
 */
data class TransportConfig(
    /**
     * Maximum payload size in bytes supported by this transport.
     * BMTP Core will fragment larger payloads to fit this MTU.
     * Examples: 
     * - BLE: typically 512 bytes
     * - TCP: typically 65535 bytes
     * - LoRa: typically 255 bytes
     */
    val mtu: Int = 512,
    
    /**
     * How long to wait before declaring a connection attempt failed.
     */
    val connectionTimeoutMs: Long = 10000L,
    
    /**
     * Whether the transport should continuously try to discover new peers.
     */
    val continuousDiscovery: Boolean = true,
    
    /**
     * A platform-specific configuration object (e.g., UUIDs for BLE).
     * Handled uniquely by each concrete Transport implementation.
     */
    val platformConfig: Map<String, Any> = emptyMap()
)
