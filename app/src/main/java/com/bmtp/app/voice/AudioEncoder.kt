package com.bmtp.app.voice

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for Audio Codecs.
 * In a production Android app, this interfaces with the native Opus C library via JNI.
 */
interface AudioEncoder {
    fun initialize(sampleRate: Int, channels: Int)
    fun setBitrate(bitrateBps: Int)
    fun encode(pcmData: ByteArray): ByteArray
    fun release()
}

/**
 * Mock implementation of an Opus encoder for architectural completeness.
 * Simulates encoding PCM data into a compressed format.
 */
@Singleton
class MockOpusEncoder @Inject constructor(
    private val logger: VoiceLogger
) : AudioEncoder {
    
    private var isInitialized = false
    private var currentBitrate = 16000

    override fun initialize(sampleRate: Int, channels: Int) {
        isInitialized = true
        logger.logEncoderStarted(sampleRate, currentBitrate)
    }

    override fun setBitrate(bitrateBps: Int) {
        currentBitrate = bitrateBps
    }

    override fun encode(pcmData: ByteArray): ByteArray {
        if (!isInitialized) throw CodecFailureException("Encoder not initialized")
        
        // Mock compression: return a byte array roughly scaled by the target bitrate
        // For 20ms frames, a 16000 bps bitrate yields roughly 40 bytes per frame.
        val targetSizeBytes = (currentBitrate * 0.02 / 8).toInt().coerceAtLeast(10)
        
        // Return a mock byte array of the target size, preserving the first few bytes for tracing
        val encoded = ByteArray(targetSizeBytes)
        if (pcmData.isNotEmpty() && encoded.isNotEmpty()) {
            encoded[0] = pcmData[0]
        }
        
        return encoded
    }

    override fun release() {
        isInitialized = false
    }
}
