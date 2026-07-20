package com.bmtp.app.protocol

/**
 * Defines the type of a BMTP packet.
 *
 * Each type serves a specific function in the protocol.
 *
 * @property byteValue The raw byte value used in the serialized packet header.
 */
enum class PacketType(val byteValue: UByte) {
    /** Hello packet, used for introducing a node to neighbors */
    HELLO(0x01u),
    
    /** Ping packet, used to check if a node is alive */
    PING(0x02u),
    
    /** Pong packet, response to a PING */
    PONG(0x03u),
    
    /** Standard message packet, carries application-level data */
    MESSAGE(0x04u),
    
    /** Acknowledgment packet, confirms receipt of a message */
    ACK(0x05u),
    
    /** Discovery packet, used to explore the mesh topology */
    DISCOVERY(0x06u),
    
    /** Route request packet, used to find a path to a specific node */
    ROUTE_REQUEST(0x07u),
    
    /** Route reply packet, returns the discovered path */
    ROUTE_REPLY(0x08u),
    
    /** File chunk packet, carries a piece of a larger file */
    FILE_CHUNK(0x09u),
    
    /** File complete packet, signals the end of a file transfer */
    FILE_COMPLETE(0x0Au),
    
    /** Key exchange packet, used to establish secure communications */
    KEY_EXCHANGE(0x0Bu),
    
    /** Heartbeat packet, periodically sent to maintain connections */
    HEARTBEAT(0x0Cu),
    
    /** Error packet, signals a protocol or application error */
    ERROR(0x0Du),
    
    /** Broadcast packet, meant to be received by all nodes */
    BROADCAST(0x0Eu),
    
    /** System packet, reserved for internal protocol operations */
    SYSTEM(0x0Fu),
    
    /** Unknown packet type, fallback for unrecognized byte values */
    UNKNOWN(0xFFu);

    companion object {
        /**
         * Resolves a byte value to a [PacketType].
         *
         * @param value The raw byte value from a packet header.
         * @return The matching [PacketType], or [UNKNOWN] if the type is not recognized.
         */
        fun fromByte(value: UByte): PacketType {
            return entries.find { it.byteValue == value } ?: UNKNOWN
        }
    }
}
