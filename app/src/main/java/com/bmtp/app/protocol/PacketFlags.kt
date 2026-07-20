package com.bmtp.app.protocol

/**
 * Value class representing the flags byte in the BMTP packet header.
 * 
 * Uses a zero-allocation inline class over a UByte to efficiently manage 8 boolean flags.
 *
 * @property bits The raw byte value containing the flags.
 */
@JvmInline
value class PacketFlags(val bits: UByte) {
    /** Whether this packet requires an acknowledgment */
    val requiresAck: Boolean
        get() = (bits and FLAG_REQUIRES_ACK) != 0u.toUByte()

    /** Whether this packet is encrypted (for future use, phase 4/5) */
    val isEncrypted: Boolean
        get() = (bits and FLAG_ENCRYPTED) != 0u.toUByte()

    /** Whether this packet is compressed */
    val isCompressed: Boolean
        get() = (bits and FLAG_COMPRESSED) != 0u.toUByte()

    /** Whether this packet is a fragment of a larger payload */
    val isFragment: Boolean
        get() = (bits and FLAG_FRAGMENT) != 0u.toUByte()
        
    /** Whether this packet should be processed with high priority */
    val isHighPriority: Boolean
        get() = (bits and FLAG_HIGH_PRIORITY) != 0u.toUByte()

    companion object {
        private const val FLAG_REQUIRES_ACK: UByte = 0x01u
        private const val FLAG_ENCRYPTED: UByte = 0x02u
        private const val FLAG_COMPRESSED: UByte = 0x04u
        private const val FLAG_FRAGMENT: UByte = 0x08u
        private const val FLAG_HIGH_PRIORITY: UByte = 0x10u

        /**
         * Builds a [PacketFlags] instance from individual boolean flags.
         *
         * @return The combined flags byte wrapped in [PacketFlags].
         */
        fun build(
            requiresAck: Boolean = false,
            isEncrypted: Boolean = false,
            isCompressed: Boolean = false,
            isFragment: Boolean = false,
            isHighPriority: Boolean = false
        ): PacketFlags {
            var flags: UByte = 0u
            if (requiresAck) flags = (flags or FLAG_REQUIRES_ACK)
            if (isEncrypted) flags = (flags or FLAG_ENCRYPTED)
            if (isCompressed) flags = (flags or FLAG_COMPRESSED)
            if (isFragment) flags = (flags or FLAG_FRAGMENT)
            if (isHighPriority) flags = (flags or FLAG_HIGH_PRIORITY)
            return PacketFlags(flags)
        }
    }
}
