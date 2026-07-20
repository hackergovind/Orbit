package com.bmtp.app.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Orchestrates AudioCapture -> AudioEncoder -> Encryption -> Packetizer -> Transport
 */
class VoiceRecorder @Inject constructor(
    private val config: VoiceConfig,
    private val audioCapture: AudioCapture,
    private val audioEncoder: MockOpusEncoder, // Using Mock for pure Kotlin
    private val voiceEncryption: VoiceEncryption,
    private val voicePacketizer: VoicePacketizer,
    private val voiceTransport: VoiceTransport,
    private val voiceScheduler: VoiceScheduler,
    private val logger: VoiceLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var recordingJob: Job? = null
    
    private val sequenceNumber = AtomicInteger(0)

    fun startRecording(
        sessionId: String,
        targetGroupIdHex: String,
        sessionKey: ByteArray,
        packetType: VoicePacketType
    ) {
        if (recordingJob != null) return

        recordingJob = scope.launch {
            try {
                audioEncoder.initialize(config.sampleRate, 1)
                audioCapture.startRecording()
                sequenceNumber.set(0)

                // The IO thread blocking read loop
                audioCapture.readFrames { pcmFrame ->
                    
                    // 1. Check if bitrate needs adapting
                    audioEncoder.setBitrate(voiceScheduler.currentBitrateBps.value)
                    
                    // 2. Encode PCM to Opus
                    val encodedPayload = audioEncoder.encode(pcmFrame)
                    
                    // 3. Encrypt
                    val encryptedPayload = voiceEncryption.encrypt(sessionKey, encodedPayload)
                    
                    // 4. Packetize
                    val seq = sequenceNumber.getAndIncrement()
                    val frame = EncodedVoiceFrame(
                        sessionId = sessionId,
                        sequenceNumber = seq,
                        timestamp = System.currentTimeMillis(),
                        packetType = packetType,
                        codecId = 0x01, // Mock Opus Codec ID
                        bitrate = voiceScheduler.currentBitrateBps.value,
                        payload = encryptedPayload
                    )
                    val rawPacket = voicePacketizer.packetize(frame)
                    
                    // 5. Transmit
                    voiceTransport.sendVoicePacket(targetGroupIdHex, rawPacket, packetType)
                }

            } catch (e: Exception) {
                logger.logError("Voice recording failed", e)
            } finally {
                cleanup()
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        cleanup()
    }

    private fun cleanup() {
        audioCapture.stopRecording()
        audioEncoder.release()
    }
}
