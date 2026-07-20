package com.bmtp.app.protocol

/**
 * Defines the BMTP Protocol versions.
 *
 * @property byteValue The raw byte value used in the serialized packet header.
 */
enum class ProtocolVersion(val byteValue: UByte) {
    /** Version 1 of the BMTP Protocol */
    V1(ProtocolConstants.CURRENT_VERSION),
    
    /** Unknown protocol version */
    UNKNOWN(0xFFu);

    companion object {
        /**
         * Resolves a byte value to a [ProtocolVersion].
         *
         * @param value The raw byte value from a packet header.
         * @return The matching [ProtocolVersion], or [UNKNOWN] if the version is not recognized.
         */
        fun fromByte(value: UByte): ProtocolVersion {
            return entries.find { it.byteValue == value } ?: UNKNOWN
        }
    }
}

/**
 * Extension to check if this protocol version is currently supported by the node.
 * 
 * @return True if supported, false if [ProtocolVersion.UNKNOWN] or unsupported.
 */
fun ProtocolVersion.isSupported(): Boolean {
    return this == ProtocolVersion.V1
}
