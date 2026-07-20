package com.bmtp.app.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import javax.inject.Inject

/**
 * Interface for deserializing a raw byte array back into a [Packet].
 */
interface PacketParser {
    /**
     * Parses a binary byte array into a [Packet].
     * Performs structural validation and checksum verification.
     *
     * @param data The raw bytes received over the network.
     * @return The parsed [Packet].
     * @throws ProtocolException if parsing fails due to invalid structure or checksum.
     */
    fun parse(data: ByteArray): Packet
}

/**
 * Implementation of [PacketParser] using little-endian byte order.
 */
class PacketParserImpl @Inject constructor() : PacketParser {

    override fun parse(data: ByteArray): Packet {
        val totalSize = data.size
        
        // 1. Basic length check
        val minSize = ProtocolConstants.HEADER_SIZE_BYTES + ProtocolConstants.FOOTER_SIZE_BYTES
        if (totalSize < minSize) {
            throw MalformedPacketException("Packet too short: $totalSize bytes (min $minSize)")
        }
        if (totalSize > ProtocolConstants.MAX_PACKET_SIZE_BYTES) {
            throw PacketTooLargeException(totalSize)
        }

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        // 2. Read Header fields
        val versionByte = buffer.get().toUByte()
        val typeByte = buffer.get().toUByte()
        val flagsByte = buffer.get().toUByte()
        val ttl = buffer.get().toUByte()
        val hopCount = buffer.get().toUByte()
        buffer.short // Skip 2 reserved bytes
        val declaredPayloadLength = buffer.short.toUShort().toInt()
        
        val packetId = ByteArray(16)
        buffer.get(packetId)
        
        val senderNodeId = ByteArray(16)
        buffer.get(senderNodeId)
        
        val receiverNodeId = ByteArray(16)
        buffer.get(receiverNodeId)
        
        val timestamp = buffer.long

        // 3. Verify payload length
        val actualPayloadLength = totalSize - minSize
        if (declaredPayloadLength != actualPayloadLength) {
            throw PayloadSizeException(declaredPayloadLength, actualPayloadLength)
        }

        // 4. Read Payload
        val payload = ByteArray(actualPayloadLength)
        if (actualPayloadLength > 0) {
            buffer.get(payload)
        }

        // 5. Read and Verify Checksum
        val declaredChecksum = buffer.int.toUInt()
        
        val bytesToCheck = data.sliceArray(0 until (ProtocolConstants.HEADER_SIZE_BYTES + actualPayloadLength))
        val crc32 = CRC32()
        crc32.update(bytesToCheck)
        val computedChecksum = crc32.value.toUInt()
        
        if (declaredChecksum != computedChecksum) {
            throw ChecksumException("Checksum mismatch: expected $declaredChecksum, got $computedChecksum")
        }

        // 6. Construct objects
        val version = ProtocolVersion.fromByte(versionByte)
        val type = PacketType.fromByte(typeByte)
        val flags = PacketFlags(flagsByte)

        val header = PacketHeader(
            version = version,
            type = type,
            flags = flags,
            ttl = ttl,
            hopCount = hopCount,
            payloadLength = declaredPayloadLength.toUShort(),
            packetId = packetId,
            senderNodeId = senderNodeId,
            receiverNodeId = receiverNodeId,
            timestamp = timestamp
        )

        return Packet(header, payload, declaredChecksum)
    }
}
