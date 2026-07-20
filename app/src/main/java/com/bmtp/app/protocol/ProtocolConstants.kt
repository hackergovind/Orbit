package com.bmtp.app.protocol

/**
 * Constants defining the BMTP packet structure and limits.
 *
 * This object centralized all protocol-level bounds and sizes to ensure
 * deterministic parsing and serialization across the mesh network.
 */
object ProtocolConstants {
    /** The protocol version (V1) */
    const val CURRENT_VERSION: UByte = 1u

    /** Total size of the fixed packet header in bytes */
    const val HEADER_SIZE_BYTES = 65

    /** Total size of the packet footer (CRC32) in bytes */
    const val FOOTER_SIZE_BYTES = 4

    /** Maximum allowed packet size (matches common BLE MTU) */
    const val MAX_PACKET_SIZE_BYTES = 512

    /** Maximum allowed payload size */
    const val MAX_PAYLOAD_SIZE_BYTES = MAX_PACKET_SIZE_BYTES - HEADER_SIZE_BYTES - FOOTER_SIZE_BYTES

    /** Minimum Time To Live (TTL) for a packet */
    const val MIN_TTL: UByte = 1u

    /** Maximum Time To Live (TTL) for a packet */
    const val MAX_TTL: UByte = 15u
    
    /** Default Time To Live for new packets */
    const val DEFAULT_TTL: UByte = 7u

    /** Broadcast Node ID (used when a packet is meant for everyone) */
    val BROADCAST_NODE_ID = ByteArray(16) { 0xFF.toByte() }

    /** Unknown Node ID */
    val UNKNOWN_NODE_ID = ByteArray(16) { 0x00.toByte() }
    
    /** Max size for the duplicate detection cache */
    const val CACHE_MAX_SIZE = 1000
    
    /** Time before a packet ID expires from the cache (in milliseconds) */
    const val CACHE_EXPIRY_MS = 60_000L
}
