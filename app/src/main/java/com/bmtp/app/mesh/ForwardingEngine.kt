package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketSerializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface that bridges the Mesh Engine with the Bluetooth Connection Layer.
 * In a real implementation, this would be backed by the Phase 2 ConnectionRepository.
 * For Phase 4 independence, we define the expected capability here.
 */
interface MeshTransport {
    /**
     * Sends a serialized packet to a specific connected neighbor.
     * @param targetDeviceId The neighbor's device ID.
     * @param data The serialized byte array.
     * @return Result indicating success or failure.
     */
    fun sendPacket(targetDeviceId: String, data: ByteArray): Result<Unit>
}

/**
 * Responsible for the physical dispatch of a packet to the chosen neighbors.
 * Serializes the packet and hands it off to the transport layer.
 */
@Singleton
class ForwardingEngine @Inject constructor(
    private val packetSerializer: PacketSerializer,
    private val policy: ForwardingPolicy,
    private val neighborManager: NeighborManager,
    private val forwardQueue: ForwardQueue,
    private val transport: MeshTransport,
    private val logger: MeshLogger,
    private val stats: MeshStatistics
) {
    /**
     * Attempts to forward a packet.
     *
     * @param packet The validated and TTL-decremented packet.
     * @param sourceDeviceId The ID of the device that sent this to us (null if we originated it).
     */
    fun forward(packet: Packet, sourceDeviceId: String?) {
        val neighbors = neighborManager.connectedNeighbors.value
        val targets = policy.selectTargets(packet, neighbors, sourceDeviceId)

        if (targets.isEmpty()) {
            // Store and forward if no neighbors are available
            forwardQueue.enqueue(packet)
            return
        }

        try {
            val serializedBytes = packetSerializer.serialize(packet)
            
            var successfulSends = 0
            for (target in targets) {
                val result = transport.sendPacket(target, serializedBytes)
                if (result.isSuccess) {
                    successfulSends++
                } else {
                    logger.logError("Failed to send packet to $target", result.exceptionOrNull())
                }
            }
            
            if (successfulSends > 0) {
                stats.incrementForwarded()
                logger.logForwarded(packet.header.packetIdAsString(), successfulSends)
            }
            
        } catch (e: Exception) {
            logger.logDropped(packet.header.packetIdAsString(), "Serialization failed: ${e.message}")
            stats.incrementDropped()
        }
    }
}
