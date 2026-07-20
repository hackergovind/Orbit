package com.bmtp.app.protocol

/**
 * Represents a complete BMTP packet, including its header, payload, and checksum.
 *
 * Instances of this class are immutable. To modify routing properties (like TTL or hop count),
 * use the provided copy functions which return a new instance.
 *
 * @property header The 65-byte fixed header.
 * @property payload The variable-length binary payload.
 * @property checksum The CRC32 checksum over the header and payload bytes.
 */
data class Packet(
    val header: PacketHeader,
    val payload: ByteArray,
    val checksum: UInt
) {
    /**
     * Checks if this packet is intended for all nodes (broadcast).
     */
    val isBroadcast: Boolean
        get() = header.receiverNodeId.contentEquals(ProtocolConstants.BROADCAST_NODE_ID)

    /**
     * Checks if this packet is meant for a specific receiver.
     */
    fun isForNode(nodeId: ByteArray): Boolean {
        return header.receiverNodeId.contentEquals(nodeId)
    }

    /**
     * Checks if this packet requires an acknowledgment from the receiver.
     */
    val requiresAck: Boolean
        get() = header.flags.requiresAck

    /**
     * Checks if the packet's Time To Live (TTL) has expired (reached 0).
     */
    val isExpired: Boolean
        get() = header.ttl <= 0u

    /**
     * Returns a new [Packet] with the TTL decremented by 1.
     * This is typically called by a routing node before forwarding the packet.
     * 
     * Note: The checksum will need to be recomputed by the serializer when sending.
     * 
     * @return A copy of this packet with a decremented TTL.
     */
    fun withDecrementedTtl(): Packet {
        if (header.ttl <= 0u) return this
        val newHeader = header.copy(ttl = (header.ttl - 1u).toUByte())
        return this.copy(header = newHeader)
    }

    /**
     * Returns a new [Packet] with the hop count incremented by 1.
     * This is typically called by a routing node before forwarding the packet.
     * 
     * Note: The checksum will need to be recomputed by the serializer when sending.
     *
     * @return A copy of this packet with an incremented hop count.
     */
    fun withIncrementedHopCount(): Packet {
        val newHeader = header.copy(hopCount = (header.hopCount + 1u).toUByte())
        return this.copy(header = newHeader)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (header != other.header) return false
        if (!payload.contentEquals(other.payload)) return false
        if (checksum != other.checksum) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + checksum.hashCode()
        return result
    }
}
