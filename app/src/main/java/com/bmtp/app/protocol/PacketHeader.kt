package com.bmtp.app.protocol

/**
 * Data class representing the 65-byte header of a BMTP packet.
 *
 * The header contains routing, identification, and validation metadata for the payload.
 *
 * @property version The protocol version.
 * @property type The type of this packet.
 * @property flags Bit flags for various packet properties.
 * @property ttl Time To Live, decremented each hop.
 * @property hopCount Number of times this packet has been relayed.
 * @property payloadLength Length of the payload in bytes (0 to 443).
 * @property packetId Unique 16-byte identifier for duplicate detection.
 * @property senderNodeId The 16-byte ID of the node that originated this packet.
 * @property receiverNodeId The 16-byte ID of the intended destination (or broadcast).
 * @property timestamp Epoch time in milliseconds when the packet was created.
 */
data class PacketHeader(
    val version: ProtocolVersion,
    val type: PacketType,
    val flags: PacketFlags,
    val ttl: UByte,
    val hopCount: UByte,
    val payloadLength: UShort,
    val packetId: ByteArray,
    val senderNodeId: ByteArray,
    val receiverNodeId: ByteArray,
    val timestamp: Long
) {
    /**
     * Helper to get the packet ID as a hex string for caching/logging.
     */
    fun packetIdAsString(): String {
        return packetId.joinToString("") { "%02x".format(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PacketHeader

        if (version != other.version) return false
        if (type != other.type) return false
        if (flags != other.flags) return false
        if (ttl != other.ttl) return false
        if (hopCount != other.hopCount) return false
        if (payloadLength != other.payloadLength) return false
        if (!packetId.contentEquals(other.packetId)) return false
        if (!senderNodeId.contentEquals(other.senderNodeId)) return false
        if (!receiverNodeId.contentEquals(other.receiverNodeId)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + flags.hashCode()
        result = 31 * result + ttl.hashCode()
        result = 31 * result + hopCount.hashCode()
        result = 31 * result + payloadLength.hashCode()
        result = 31 * result + packetId.contentHashCode()
        result = 31 * result + senderNodeId.contentHashCode()
        result = 31 * result + receiverNodeId.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
