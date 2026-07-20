package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.ProtocolException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exception thrown when a packet exceeds the maximum allowed hop count.
 */
class HopLimitExceededException(val packetId: String) : ProtocolException("Hop limit exceeded for packet: $packetId")

/**
 * Manages packet hop counts to prevent infinite loops even if TTL is misconfigured.
 */
@Singleton
class HopManager @Inject constructor(
    private val config: MeshConfig
) {
    /**
     * Validates that the current hop count does not exceed the maximum allowed.
     *
     * @param hopCount The current hop count of the packet.
     * @throws HopLimitExceededException if the limit is exceeded.
     */
    fun validate(hopCount: UByte) {
        if (hopCount > config.maxHopCount) {
            throw HopLimitExceededException("unknown")
        }
    }

    /**
     * Increments the hop count of a packet before forwarding.
     *
     * @param packet The packet to process.
     * @return A new copy of the packet with incremented hop count.
     * @throws HopLimitExceededException if incrementing would exceed the limit.
     */
    fun incrementHopCount(packet: Packet): Packet {
        if (packet.header.hopCount >= config.maxHopCount) {
            throw HopLimitExceededException(packet.header.packetIdAsString())
        }
        
        return packet.withIncrementedHopCount()
    }
}
