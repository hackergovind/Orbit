package com.bmtp.app.voice

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized logger for the Voice Communication Layer.
 * Ensures strict compliance: "Never log audio content."
 */
@Singleton
class VoiceLogger @Inject constructor() {
    private val subtag = "VoiceMesh"

    fun logSessionStarted(sessionId: String, type: String) {
        LogUtils.i(subtag, "Voice Session Started: $sessionId | Type: $type")
    }

    fun logSessionEnded(sessionId: String, reason: String) {
        LogUtils.i(subtag, "Voice Session Ended: $sessionId | Reason: $reason")
    }

    fun logEncoderStarted(sampleRate: Int, bitrate: Int) {
        LogUtils.d(subtag, "Encoder Started | SR: $sampleRate Hz | BR: $bitrate bps")
    }

    fun logDecoderStarted(sampleRate: Int) {
        LogUtils.d(subtag, "Decoder Started | SR: $sampleRate Hz")
    }

    fun logBitrateChanged(oldBitrate: Int, newBitrate: Int, reason: String) {
        LogUtils.w(subtag, "Bitrate Changed | $oldBitrate -> $newBitrate bps | Reason: $reason")
    }

    fun logPacketLossDetected(sequenceNumber: Int, estimatedLossRate: Float) {
        LogUtils.w(subtag, "Packet Loss Detected | Seq: $sequenceNumber | Est Loss Rate: ${estimatedLossRate * 100}%")
    }
    
    fun logJitterBufferUnderrun() {
        LogUtils.w(subtag, "Jitter Buffer Underrun - Starvation detected")
    }
    
    fun logJitterBufferOverflow(droppedSeq: Int) {
        LogUtils.w(subtag, "Jitter Buffer Overflow - Dropping Seq: $droppedSeq")
    }

    fun logReconnection(sessionId: String, attempt: Int) {
        LogUtils.i(subtag, "Reconnecting Voice Session: $sessionId | Attempt: $attempt")
    }

    fun logError(message: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, message, throwable)
    }
    
    // TRACE logs for debugging packet flow without logging content
    fun logPacketSent(sessionId: String, seq: Int, size: Int) {
        LogUtils.v(subtag, "TX -> Session: $sessionId | Seq: $seq | Size: $size bytes")
    }

    fun logPacketReceived(sessionId: String, seq: Int, size: Int) {
        LogUtils.v(subtag, "RX <- Session: $sessionId | Seq: $seq | Size: $size bytes")
    }
}
