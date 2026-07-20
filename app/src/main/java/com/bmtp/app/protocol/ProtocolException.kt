package com.bmtp.app.protocol

/**
 * Base exception class for all errors related to the BMTP protocol.
 */
sealed class ProtocolException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when a packet fails basic structure validation.
 */
class InvalidPacketException(message: String) : ProtocolException(message)

/**
 * Thrown when the calculated CRC32 checksum does not match the checksum in the packet footer.
 */
class ChecksumException(message: String = "Packet checksum validation failed") : ProtocolException(message)

/**
 * Thrown when a packet uses a protocol version that is not supported by this node.
 */
class VersionMismatchException(val version: ProtocolVersion) : 
    ProtocolException("Unsupported protocol version: $version")

/**
 * Thrown when a packet exceeds the maximum allowed size (e.g., 512 bytes).
 */
class PacketTooLargeException(val size: Int) : 
    ProtocolException("Packet size ($size bytes) exceeds maximum allowed (${ProtocolConstants.MAX_PACKET_SIZE_BYTES} bytes)")

/**
 * Thrown when a packet is structurally malformed (e.g., too short to contain a header).
 */
class MalformedPacketException(message: String) : ProtocolException(message)

/**
 * Thrown when a packet type is unknown or explicitly unsupported by this node.
 */
class UnsupportedPacketException(val type: PacketType) : 
    ProtocolException("Unsupported packet type: $type")

/**
 * Thrown when a packet is rejected because it has already been processed recently.
 */
class DuplicatePacketException(val packetId: String) : 
    ProtocolException("Duplicate packet detected: $packetId")

/**
 * Thrown when a packet is rejected because its Time To Live (TTL) has reached 0.
 */
class TtlExpiredException(val packetId: String) : 
    ProtocolException("Packet TTL expired: $packetId")

/**
 * Thrown when the declared payload size in the header does not match the actual payload size.
 */
class PayloadSizeException(val declared: Int, val actual: Int) : 
    ProtocolException("Payload size mismatch: declared $declared, actual $actual")
