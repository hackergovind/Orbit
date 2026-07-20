package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.ProtocolException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the modification and forwarding of packets not destined for this node.
 */
@Singleton
class RelayEngine @Inject constructor(
    private val ttlManager: TTLManager,
    private val hopManager: HopManager,
    private val forwardingEngine: ForwardingEngine,
    private val logger: MeshLogger,
    private val stats: MeshStatistics
) {
    /**
     * Processes a packet for relaying.
     * Decrements TTL, increments Hop Count, and hands off to the ForwardingEngine.
     *
     * @param packet The packet to relay.
     * @param sourceDeviceId The device ID from which the packet was received.
     */
    fun relayPacket(packet: Packet, sourceDeviceId: String?) {
        try {
            // 1. Decrement TTL
            val ttlDecremented = ttlManager.decrementTtl(packet)
            
            // 2. Increment Hop Count
            val readyToForward = hopManager.incrementHopCount(ttlDecremented)
            
            // 3. Dispatch to Forwarding Engine
            forwardingEngine.forward(readyToForward, sourceDeviceId)
            
        } catch (e: ProtocolException) {
            // e.g. TtlExpiredException or HopLimitExceededException
            logger.logDropped(packet.header.packetIdAsString(), e.message ?: "Protocol Exception during relay")
            stats.incrementDropped()
        } catch (e: Exception) {
            logger.logDropped(packet.header.packetIdAsString(), "Unexpected error during relay: ${e.message}")
            stats.incrementDropped()
        }
    }
}
