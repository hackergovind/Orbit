package com.bmtp.app.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates Transport -> Depacketizer -> JitterBuffer -> Encryption -> AudioDecoder -> AudioTrack
 */
@Singleton
class VoicePlayer @Inject constructor(
    private val config: VoiceConfig,
    private val voiceTransport: VoiceTransport,
    private val voiceDepacketizer: VoiceDepacketizer,
    private val jitterBuffer: JitterBuffer,
    private val voiceEncryption: VoiceEncryption,
    private val audioDecoder: MockOpusDecoder,
    private val logger: VoiceLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var playbackJob: Job? = null
    
    private var audioTrack: AudioTrack? = null
    private var sessionKey: ByteArray? = null

    init {
        // Hook into the transport layer
        voiceTransport.onVoicePacketReceived = { rawPacket ->
            handleIncomingNetworkPacket(rawPacket)
        }
    }

    /**
     * Prepares the audio hardware and starts the playback loop pumping from the JitterBuffer.
     */
    fun startPlayback(key: ByteArray) {
        if (playbackJob != null) return
        
        sessionKey = key
        audioDecoder.initialize(config.sampleRate, 1)
        
        val minBufferSize = AudioTrack.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(config.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            
        audioTrack?.play()
        
        playbackJob = scope.launch(Dispatchers.IO) {
            playbackLoop()
        }
    }
    
    private suspend fun playbackLoop() {
        while (isActive) {
            try {
                // 1. Pop from Jitter Buffer
                val frame = jitterBuffer.pop()
                
                if (frame == null) {
                    // Buffer is starved OR packet was lost.
                    // A real Opus decoder handles PLC if we tell it a packet is missing.
                    val plcAudio = audioDecoder.decodeMissingPacket()
                    audioTrack?.write(plcAudio, 0, plcAudio.size)
                    
                    // Small delay to prevent tight loop spinning when severely starved
                    delay(config.frameDurationMs.toLong())
                    continue
                }
                
                // 2. Decrypt
                val key = sessionKey ?: throw VoiceSessionException("No session key for playback")
                val decryptedPayload = voiceEncryption.decrypt(key, frame.payload)
                
                // 3. Decode
                val pcmAudio = audioDecoder.decode(decryptedPayload)
                
                // 4. Play
                audioTrack?.write(pcmAudio, 0, pcmAudio.size)
                
            } catch (e: Exception) {
                logger.logError("Error in playback loop", e)
                delay(10)
            }
        }
    }
    
    private fun handleIncomingNetworkPacket(rawPacket: ByteArray) {
        try {
            val frame = voiceDepacketizer.depacketize(rawPacket)
            jitterBuffer.push(frame)
        } catch (e: Exception) {
            logger.logError("Failed to handle incoming voice packet", e)
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        
        jitterBuffer.flush()
        audioDecoder.release()
        
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
