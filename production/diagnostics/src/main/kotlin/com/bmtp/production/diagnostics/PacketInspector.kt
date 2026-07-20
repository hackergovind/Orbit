package com.bmtp.production.diagnostics

/**
 * A diagnostic utility to print human-readable explanations of a raw packet's header.
 * Crucially, it does NOT attempt to parse or print the encrypted payload.
 */
object PacketInspector {

    fun inspectHeader(rawBytes: ByteArray): String {
        if (rawBytes.size < 32) return "ERROR: Packet too short to contain a valid header."

        val magic1 = rawBytes[0].toInt()
        val magic2 = rawBytes[1].toInt()
        val version = rawBytes[2].toInt()
        val type = rawBytes[3].toInt()
        val flags = rawBytes[4].toInt()
        val ttl = rawBytes[5].toInt()
        val hopCount = rawBytes[6].toInt()

        val typeName = when (type) {
            0x01 -> "HANDSHAKE_INIT"
            0x02 -> "HANDSHAKE_ACK"
            0x03 -> "ROUTING_RREQ"
            0x04 -> "ROUTING_RREP"
            0x05 -> "DATA_MESSAGE"
            0x06 -> "DATA_ACK"
            0x07 -> "VOICE_FRAME"
            0x08 -> "FILE_CHUNK"
            else -> "UNKNOWN ($type)"
        }

        val ackReq = (flags and 0x01) != 0
        val isFrag = (flags and 0x02) != 0

        return """
            --- ANTIGRAVITY PACKET HEADER ---
            Magic: ${magic1.toString(16)} ${magic2.toString(16)} (Expected 41 47)
            Version: $version
            Type: $typeName
            Flags: ACK_REQ=$ackReq, IS_FRAG=$isFrag
            TTL: $ttl
            Hops: $hopCount
            Payload Size: ${rawBytes.size - 96} bytes (Encrypted)
            ---------------------------------
        """.trimIndent()
    }
}
