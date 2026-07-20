package com.bmtp.app.voice

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for Audio Codecs.
 * In a production Android app, this interfaces with the native Opus C library via JNI.
 */
interface AudioDecoder {
    fun initialize(sampleRate: Int, channels: Int)
    
    /**
     * Decodes a compressed Opus frame back into raw PCM audio.
     */
    fun decode(encodedData: ByteArray): ByteArray
    
    /**
     * Triggers Packet Loss Concealment (PLC).
     * Call this when a packet is lost to have the codec guess/interpolate the missing audio.
     */
    fun decodeMissingPacket(): ByteArray
    
    fun release()
}

/**
 * Mock implementation of an Opus decoder for architectural completeness.
 */
@Singleton
class MockOpusDecoder @Inject constructor(
    private val logger: VoiceLogger,
    private val config: VoiceConfig
) : AudioDecoder {
    
    private var isInitialized = false

    override fun initialize(sampleRate: Int, channels: Int) {
        isInitialized = true
        logger.logDecoderStarted(sampleRate)
    }

    override fun decode(encodedData: ByteArray): ByteArray {
        if (!isInitialized) throw CodecFailureException("Decoder not initialized")
        
        // Mock decompression: return a full-size PCM buffer
        val decoded = ByteArray(config.pcmFrameSizeBytes)
        if (encodedData.isNotEmpty() && decoded.isNotEmpty()) {
            decoded[0] = encodedData[0]
        }
        
        return decoded
    }

    override fun decodeMissingPacket(): ByteArray {
        if (!isInitialized) throw CodecFailureException("Decoder not initialized")
        
        // Mock PLC (Packet Loss Concealment)
        // A real Opus codec uses the previous frame's frequency characteristics to guess this frame.
        return ByteArray(config.pcmFrameSizeBytes) // Empty frame (silence) as mock
    }

    override fun release() {
        isInitialized = false
    }
}
