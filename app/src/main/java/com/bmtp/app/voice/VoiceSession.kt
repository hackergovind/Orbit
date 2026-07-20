package com.bmtp.app.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Represents a single active voice call or Push-To-Talk channel connection.
 */
class VoiceSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val targetGroupIdHex: String,
    private val sessionKey: ByteArray, // Secure key from Phase 6/9
    private val voiceRecorder: VoiceRecorder,
    private val voicePlayer: VoicePlayer,
    private val voicePresence: VoicePresence,
    private val voiceScheduler: VoiceScheduler,
    private val stats: VoiceStatistics,
    private val logger: VoiceLogger,
    private val myMemberIdHex: String
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var schedulerJob: Job? = null
    
    var isActiveCall: Boolean = false
        private set
        
    fun start() {
        if (isActiveCall) return
        isActiveCall = true
        
        logger.logSessionStarted(sessionId, "LiveVoice")
        
        // 1. Start listening to incoming audio
        voicePlayer.startPlayback(sessionKey)
        
        // 2. Mark self as listening
        voicePresence.updateState(sessionId, myMemberIdHex, VoiceState.LISTENING)
        
        // 3. Start adaptive bitrate scheduler loop
        schedulerJob = scope.launch {
            while (isActive) {
                delay(5000) // Evaluate network every 5 seconds
                voiceScheduler.evaluateNetworkConditions()
            }
        }
        
        stats.updateActiveSessions(1)
    }

    /**
     * Used for Push-To-Talk. Begins capturing and transmitting.
     */
    fun startTransmitting() {
        if (!isActiveCall) throw VoiceSessionException("Session not active")
        
        voicePresence.updateState(sessionId, myMemberIdHex, VoiceState.SPEAKING)
        
        // Note: For PTT, we send LIVE_VOICE packets.
        voiceRecorder.startRecording(
            sessionId = sessionId,
            targetGroupIdHex = targetGroupIdHex,
            sessionKey = sessionKey,
            packetType = VoicePacketType.LIVE_VOICE
        )
    }

    /**
     * Used for Push-To-Talk. Stops capturing.
     */
    fun stopTransmitting() {
        if (!isActiveCall) return
        
        voiceRecorder.stopRecording()
        voicePresence.updateState(sessionId, myMemberIdHex, VoiceState.LISTENING)
    }

    fun end() {
        if (!isActiveCall) return
        isActiveCall = false
        
        schedulerJob?.cancel()
        schedulerJob = null
        
        stopTransmitting()
        voicePlayer.stopPlayback()
        
        voicePresence.clearSession(sessionId)
        voiceScheduler.resetToDefault()
        
        stats.updateActiveSessions(0)
        logger.logSessionEnded(sessionId, "User Terminated")
    }
}
