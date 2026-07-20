package com.bmtp.app.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import javax.inject.Inject

/**
 * Interface for serializing a [Packet] into a raw byte array for Bluetooth transport.
 */
interface PacketSerializer {
    /**
     * Serializes a packet into a binary byte array.
     * Computes the CRC32 checksum dynamically over the serialized header and payload.
     *
     * @param packet The packet to serialize.
     * @return The serialized byte array.
     * @throws PacketTooLargeException if the resulting payload exceeds limits.
     */
    fun serialize(packet: Packet): ByteArray
}

/**
 * Implementation of [PacketSerializer] using little-endian byte order.
 */
class PacketSerializerImpl @Inject constructor() : PacketSerializer {

    override fun serialize(packet: Packet): ByteArray {
        val payloadLength = packet.payload.size
        
        if (payloadLength > ProtocolConstants.MAX_PAYLOAD_SIZE_BYTES) {
            throw PacketTooLargeException(packet.header.payloadLength.toInt())
        }

        val totalSize = ProtocolConstants.HEADER_SIZE_BYTES + payloadLength + ProtocolConstants.FOOTER_SIZE_BYTES
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        val header = packet.header
        
        // 1. Write Header (65 bytes)
        buffer.put(header.version.byteValue.toByte())
        buffer.put(header.type.byteValue.toByte())
        buffer.put(header.flags.bits.toByte())
        buffer.put(header.ttl.toByte())
        buffer.put(header.hopCount.toByte())
        buffer.putShort(0) // Reserved 2 bytes
        buffer.putShort(payloadLength.toShort()) // Override any declared length to match actual
        
        require(header.packetId.size == 16) { "Packet ID must be exactly 16 bytes" }
        buffer.put(header.packetId)
        
        require(header.senderNodeId.size == 16) { "Sender Node ID must be exactly 16 bytes" }
        buffer.put(header.senderNodeId)
        
        require(header.receiverNodeId.size == 16) { "Receiver Node ID must be exactly 16 bytes" }
        buffer.put(header.receiverNodeId)
        
        buffer.putLong(header.timestamp)

        // 2. Write Payload
        if (payloadLength > 0) {
            buffer.put(packet.payload)
        }

        // 3. Compute and Write CRC32 Checksum
        val bytesSoFar = buffer.array().sliceArray(0 until (ProtocolConstants.HEADER_SIZE_BYTES + payloadLength))
        val crc32 = CRC32()
        crc32.update(bytesSoFar)
        val computedChecksum = crc32.value.toUInt()
        
        buffer.putInt(computedChecksum.toInt())

        return buffer.array()
    }
}
