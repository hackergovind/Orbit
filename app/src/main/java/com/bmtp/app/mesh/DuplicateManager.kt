package com.bmtp.app.mesh

import com.bmtp.app.protocol.PacketCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the protocol-level PacketCache to provide a mesh-specific interface
 * for checking if a packet has already been routed by this node.
 */
@Singleton
class DuplicateManager @Inject constructor(
    private val packetCache: PacketCache,
    private val stats: MeshStatistics
) {
    /**
     * Checks if a packet is a duplicate. If it is NOT a duplicate, it is added
     * to the underlying cache to prevent future retransmissions of the same packet.
     *
     * @param packetIdHex The 16-byte packet ID as a hex string.
     * @return True if duplicate, false if new.
     */
    fun isDuplicate(packetIdHex: String): Boolean {
        val duplicate = packetCache.isDuplicateOrAdd(packetIdHex)
        if (duplicate) {
            stats.incrementDuplicates()
        }
        return duplicate
    }
}
