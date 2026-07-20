package com.bmtp.app.voice

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reconstructs EncodedVoiceFrames from raw byte arrays received from the network.
 */
@Singleton
class VoiceDepacketizer @Inject constructor(
    private val logger: VoiceLogger
) {
    /**
     * Parses a byte array into an EncodedVoiceFrame.
     * Throws VoiceException if the packet is malformed.
     */
    fun depacketize(data: ByteArray): EncodedVoiceFrame {
        try {
            val buffer = ByteBuffer.wrap(data)
            
            val sessionIdLen = buffer.get().toInt()
            if (sessionIdLen <= 0 || sessionIdLen > 100) {
                throw VoiceException("Invalid Session ID length: $sessionIdLen")
            }
            
            val sessionIdBytes = ByteArray(sessionIdLen)
            buffer.get(sessionIdBytes)
            val sessionId = String(sessionIdBytes, Charsets.UTF_8)
            
            val sequenceNumber = buffer.getInt()
            val timestamp = buffer.getLong()
            
            val packetTypeCode = buffer.get()
            val packetType = VoicePacketType.values().find { it.code == packetTypeCode } 
                ?: throw VoiceException("Unknown packet type: $packetTypeCode")
                
            val codecId = buffer.get()
            val bitrate = buffer.getInt()
            
            val payloadLen = buffer.getInt()
            if (payloadLen < 0 || payloadLen > buffer.remaining()) {
                throw VoiceException("Invalid payload length: $payloadLen")
            }
            
            val payload = ByteArray(payloadLen)
            buffer.get(payload)
            
            return EncodedVoiceFrame(
                sessionId = sessionId,
                sequenceNumber = sequenceNumber,
                timestamp = timestamp,
                packetType = packetType,
                codecId = codecId,
                bitrate = bitrate,
                payload = payload
            )
        } catch (e: BufferUnderflowException) {
            logger.logError("Malformed voice packet: Buffer underflow", e)
            throw VoiceException("Malformed voice packet", e)
        }
    }
}
