package com.bmtp.app.voice

import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

enum class VoicePacketType(val code: Byte) {
    LIVE_VOICE(0x01),
    VOICE_NOTE(0x02),
    CONTROL_MUTE(0x03),
    CONTROL_UNMUTE(0x04)
}

data class EncodedVoiceFrame(
    val sessionId: String,
    val sequenceNumber: Int,
    val timestamp: Long,
    val packetType: VoicePacketType,
    val codecId: Byte,
    val bitrate: Int,
    val payload: ByteArray
)

/**
 * Serializes encoded audio frames into a byte array suitable for network transport.
 */
@Singleton
class VoicePacketizer @Inject constructor() {
    
    /**
     * Converts an EncodedVoiceFrame into a byte array.
     * Note: In a real system, Protobufs or highly packed binary formats are preferred.
     * We simulate a packed binary format here for efficiency.
     */
    fun packetize(frame: EncodedVoiceFrame): ByteArray {
        val sessionIdBytes = frame.sessionId.toByteArray(Charsets.UTF_8)
        val sessionIdLen = sessionIdBytes.size
        
        // Header size: 
        // SessionIdLen (1) + SessionId (N) + SeqNum (4) + Timestamp (8) + 
        // PacketType (1) + CodecId (1) + Bitrate (4) + PayloadLen (4)
        val capacity = 1 + sessionIdLen + 4 + 8 + 1 + 1 + 4 + 4 + frame.payload.size
        
        val buffer = ByteBuffer.allocate(capacity)
        
        buffer.put(sessionIdLen.toByte())
        buffer.put(sessionIdBytes)
        buffer.putInt(frame.sequenceNumber)
        buffer.putLong(frame.timestamp)
        buffer.put(frame.packetType.code)
        buffer.put(frame.codecId)
        buffer.putInt(frame.bitrate)
        buffer.putInt(frame.payload.size)
        buffer.put(frame.payload)
        
        return buffer.array()
    }
}
