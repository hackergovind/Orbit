package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.TtlExpiredException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces Time To Live (TTL) rules for the mesh network.
 */
@Singleton
class TTLManager @Inject constructor(
    private val stats: MeshStatistics
) {
    /**
     * Decrements the TTL of a packet before forwarding.
     *
     * @param packet The packet to process.
     * @return A new copy of the packet with decremented TTL.
     * @throws TtlExpiredException if the TTL reaches 0 before forwarding.
     */
    fun decrementTtl(packet: Packet): Packet {
        if (packet.isExpired) {
            stats.incrementTtlExpired()
            throw TtlExpiredException(packet.header.packetIdAsString())
        }
        
        val decrementedPacket = packet.withDecrementedTtl()
        
        // Check again after decrementing
        if (decrementedPacket.isExpired) {
            stats.incrementTtlExpired()
            throw TtlExpiredException(packet.header.packetIdAsString())
        }
        
        return decrementedPacket
    }
}
