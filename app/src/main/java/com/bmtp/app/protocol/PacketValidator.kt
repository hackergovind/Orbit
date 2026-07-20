package com.bmtp.app.protocol

import javax.inject.Inject

/**
 * Interface for validating parsed packets before they are processed by the application or routed.
 */
interface PacketValidator {
    /**
     * Validates a parsed packet against protocol rules (version, TTL, duplicates).
     * If validation succeeds, the packet ID is registered in the duplicate cache.
     *
     * @param packet The packet to validate.
     * @throws ProtocolException if the packet violates any rule.
     */
    fun validate(packet: Packet)
}

/**
 * Implementation of [PacketValidator] enforcing BMTP rules.
 */
class PacketValidatorImpl @Inject constructor(
    private val packetCache: PacketCache
) : PacketValidator {

    override fun validate(packet: Packet) {
        val header = packet.header
        
        // 1. Version check
        if (!header.version.isSupported()) {
            throw VersionMismatchException(header.version)
        }
        
        // 2. Type check
        if (header.type == PacketType.UNKNOWN) {
            throw UnsupportedPacketException(header.type)
        }
        
        // 3. TTL check
        if (header.ttl <= 0u) {
            throw TtlExpiredException(header.packetIdAsString())
        }
        
        // 4. Duplicate detection (This must be the LAST step to avoid caching invalid packets)
        val packetIdHex = header.packetIdAsString()
        if (packetCache.isDuplicateOrAdd(packetIdHex)) {
            throw DuplicatePacketException(packetIdHex)
        }
    }
}
