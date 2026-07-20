package com.bmtp.app.transport

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles generating ACKs for incoming reliable packets and correlating incoming ACKs with sent packets.
 */
@Singleton
class AckManager @Inject constructor(
    private val packetFactory: PacketFactory,
    private val retryManager: RetryManager,
    private val deliveryTracker: DeliveryTracker,
    private val stats: TransportStatistics,
    private val logger: TransportLogger
) {
    // Tracks unacknowledged packets: packetIdHex -> original Packet
    private val unackedPackets = ConcurrentHashMap<String, Packet>()

    fun registerSentPacket(packet: Packet) {
        if (packet.header.flags.requiresAck) {
            val packetIdHex = packet.header.packetId.joinToString("") { "%02x".format(it) }
            unackedPackets[packetIdHex] = packet
            retryManager.scheduleRetry(packetIdHex, packet)
        }
    }

    /**
     * Called when an ACK is received.
     * @param packetIdHex The ID of the packet that was acknowledged.
     * @param latencyMs Measured latency from send to ACK (if tracked, else 0).
     */
    fun onAckReceived(packetIdHex: String, latencyMs: Long = 0) {
        val originalPacket = unackedPackets.remove(packetIdHex)
        if (originalPacket != null) {
            stats.recordAckReceived()
            if (latencyMs > 0) stats.recordLatency(latencyMs)
            
            logger.logAckReceived(packetIdHex)
            deliveryTracker.updateState(packetIdHex, DeliveryState.ACKNOWLEDGED)
            retryManager.cancelRetry(packetIdHex)
        }
    }

    /**
     * Generates an ACK packet for a given received message.
     */
    fun createAckFor(myId: ByteArray, receivedPacket: Packet): Packet {
        return packetFactory.createAck(
            senderId = myId,
            receiverId = receivedPacket.header.senderNodeId,
            originalPacketId = receivedPacket.header.packetId
        )
    }
}
