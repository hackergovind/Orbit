package com.bmtp.app.protocol

import java.security.SecureRandom
import javax.inject.Inject

/**
 * Interface for constructing well-formed BMTP packets.
 *
 * This factory ensures that all packets have valid IDs, timestamps, and default TTLs,
 * preventing developers from manually constructing invalid headers.
 */
interface PacketFactory {
    /** Creates a HELLO packet to introduce this node to neighbors. */
    fun createHello(senderId: ByteArray, payload: ByteArray = ByteArray(0)): Packet
    
    /** Creates a PING packet to check if a specific node is alive. */
    fun createPing(senderId: ByteArray, receiverId: ByteArray): Packet
    
    /** Creates an ACK packet to confirm receipt of a message. */
    fun createAck(senderId: ByteArray, receiverId: ByteArray, originalPacketId: ByteArray): Packet
    
    /** Creates a standard MESSAGE packet carrying application data. */
    fun createMessage(senderId: ByteArray, receiverId: ByteArray, payload: ByteArray, requiresAck: Boolean = true): Packet
    
    /** Creates a DISCOVERY packet to explore the mesh topology. */
    fun createDiscovery(senderId: ByteArray): Packet
    
    /** Creates a ROUTE_REQUEST packet to find a path to a specific node. */
    fun createRouteRequest(senderId: ByteArray, payload: ByteArray): Packet
    
    /** Creates a ROUTE_REPLY packet to answer a route request. */
    fun createRouteReply(senderId: ByteArray, receiverId: ByteArray, payload: ByteArray): Packet
    
    /** Creates a ROUTE_ERROR packet to notify of broken links. */
    fun createRouteError(senderId: ByteArray, payload: ByteArray): Packet
    
    /** Creates a BROADCAST packet meant for all nodes in the mesh. */
    fun createBroadcast(senderId: ByteArray, payload: ByteArray): Packet
}

/**
 * Implementation of [PacketFactory].
 */
class PacketFactoryImpl @Inject constructor() : PacketFactory {

    private val secureRandom = SecureRandom()

    private fun generatePacketId(): ByteArray {
        val id = ByteArray(16)
        secureRandom.nextBytes(id)
        return id
    }

    private fun createBasePacket(
        type: PacketType,
        senderId: ByteArray,
        receiverId: ByteArray,
        payload: ByteArray = ByteArray(0),
        flags: PacketFlags = PacketFlags(0u)
    ): Packet {
        if (payload.size > ProtocolConstants.MAX_PAYLOAD_SIZE_BYTES) {
            throw PacketTooLargeException(ProtocolConstants.HEADER_SIZE_BYTES + payload.size + ProtocolConstants.FOOTER_SIZE_BYTES)
        }

        val header = PacketHeader(
            version = ProtocolVersion.V1,
            type = type,
            flags = flags,
            ttl = ProtocolConstants.DEFAULT_TTL,
            hopCount = 0u,
            payloadLength = payload.size.toUShort(),
            packetId = generatePacketId(),
            senderNodeId = senderId,
            receiverNodeId = receiverId,
            timestamp = System.currentTimeMillis()
        )

        // Checksum is 0 initially, it will be dynamically computed by the PacketSerializer
        return Packet(header, payload, 0u)
    }

    override fun createHello(senderId: ByteArray, payload: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.HELLO,
            senderId = senderId,
            receiverId = ProtocolConstants.BROADCAST_NODE_ID,
            payload = payload
        )
    }

    override fun createPing(senderId: ByteArray, receiverId: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.PING,
            senderId = senderId,
            receiverId = receiverId
        )
    }

    override fun createAck(senderId: ByteArray, receiverId: ByteArray, originalPacketId: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.ACK,
            senderId = senderId,
            receiverId = receiverId,
            payload = originalPacketId
        )
    }

    override fun createMessage(
        senderId: ByteArray,
        receiverId: ByteArray,
        payload: ByteArray,
        requiresAck: Boolean
    ): Packet {
        val flags = PacketFlags.build(requiresAck = requiresAck)
        return createBasePacket(
            type = PacketType.MESSAGE,
            senderId = senderId,
            receiverId = receiverId,
            payload = payload,
            flags = flags
        )
    }

    override fun createDiscovery(senderId: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.DISCOVERY,
            senderId = senderId,
            receiverId = ProtocolConstants.BROADCAST_NODE_ID
        )
    }

    override fun createRouteRequest(senderId: ByteArray, payload: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.ROUTE_REQUEST,
            senderId = senderId,
            receiverId = ProtocolConstants.BROADCAST_NODE_ID,
            payload = payload
        )
    }

    override fun createRouteReply(senderId: ByteArray, receiverId: ByteArray, payload: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.ROUTE_REPLY,
            senderId = senderId,
            receiverId = receiverId,
            payload = payload
        )
    }

    override fun createRouteError(senderId: ByteArray, payload: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.ERROR,
            senderId = senderId,
            receiverId = ProtocolConstants.BROADCAST_NODE_ID,
            payload = payload
        )
    }

    override fun createBroadcast(senderId: ByteArray, payload: ByteArray): Packet {
        return createBasePacket(
            type = PacketType.BROADCAST,
            senderId = senderId,
            receiverId = ProtocolConstants.BROADCAST_NODE_ID,
            payload = payload
        )
    }
}
