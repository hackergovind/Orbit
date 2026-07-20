package com.bmtp.app.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles raw audio capture from the device microphone.
 * Applies hardware Acoustic Echo Cancellation (AEC) and Noise Suppression (NS) if available.
 */
@Singleton
class AudioCapture @Inject constructor(
    private val config: VoiceConfig,
    private val logger: VoiceLogger
) {
    private var audioRecord: AudioRecord? = null
    
    // Hardware Effects
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    private val minBufferSize = AudioRecord.getMinBufferSize(
        config.sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    @SuppressLint("MissingPermission") // Assume permission handled at UI layer
    fun startRecording(): AudioRecord {
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw AudioCaptureException("Audio record configuration not supported by hardware")
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, // Optimized for VoIP (includes built-in AEC on many devices)
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 4 // Use a larger buffer to prevent overruns
        )

        val record = audioRecord ?: throw AudioCaptureException("Failed to initialize AudioRecord")

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            throw AudioCaptureException("AudioRecord initialization failed")
        }

        enableHardwareEffects(record.audioSessionId)

        record.startRecording()
        logger.logEncoderStarted(config.sampleRate, config.baseBitrateBps)
        return record
    }

    private fun enableHardwareEffects(sessionId: Int) {
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId)
                aec?.enabled = true
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(sessionId)
                ns?.enabled = true
            }
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(sessionId)
                agc?.enabled = true
            }
        } catch (e: Exception) {
            logger.logError("Failed to initialize hardware audio effects", e)
        }
    }

    suspend fun readFrames(onFrameCaptured: suspend (ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        val record = audioRecord ?: return@withContext
        val frameSize = config.pcmFrameSizeBytes
        val buffer = ByteArray(frameSize)

        while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val readResult = record.read(buffer, 0, frameSize)
            if (readResult > 0) {
                // Pass a copy so the buffer can be reused safely
                onFrameCaptured(buffer.copyOf(readResult))
            } else if (readResult < 0) {
                logger.logError("Error reading audio data: $readResult")
            }
        }
    }

    fun stopRecording() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            
            aec?.release()
            ns?.release()
            agc?.release()
            
            audioRecord = null
            aec = null
            ns = null
            agc = null
        } catch (e: Exception) {
            logger.logError("Error stopping audio capture", e)
        }
    }
}
