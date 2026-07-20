package com.bmtp.app.voice

import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main application facade for the Voice Communication Layer.
 */
@Singleton
class VoiceManager @Inject constructor(
    private val config: VoiceConfig,
    private val voiceRecorder: VoiceRecorder,
    private val voicePlayer: VoicePlayer,
    private val voicePresence: VoicePresence,
    private val voiceScheduler: VoiceScheduler,
    private val stats: VoiceStatistics,
    private val logger: VoiceLogger
) {
    private val activeSessions = ConcurrentHashMap<String, VoiceSession>()
    
    val presenceFlow: StateFlow<Map<String, Map<String, ParticipantVoiceState>>>
        get() = voicePresence.presenceFlow
        
    val statisticsFlow: StateFlow<VoiceMetrics>
        get() = stats.metrics

    /**
     * Joins or creates a Live Voice Session for a specific Group or 1-to-1 channel.
     */
    fun joinLiveVoice(
        targetGroupIdHex: String, 
        sessionKey: ByteArray, 
        myMemberIdHex: String
    ): VoiceSession {
        // Only allow one active live voice session at a time for battery/network reasons
        activeSessions.values.forEach { it.end() }
        activeSessions.clear()
        
        val session = VoiceSession(
            targetGroupIdHex = targetGroupIdHex,
            sessionKey = sessionKey,
            voiceRecorder = voiceRecorder,
            voicePlayer = voicePlayer,
            voicePresence = voicePresence,
            voiceScheduler = voiceScheduler,
            stats = stats,
            logger = logger,
            myMemberIdHex = myMemberIdHex
        )
        
        activeSessions[session.sessionId] = session
        session.start()
        
        return session
    }

    /**
     * Leaves an active live voice session.
     */
    fun leaveLiveVoice(sessionId: String) {
        val session = activeSessions.remove(sessionId)
        session?.end()
    }

    /**
     * Records and sends an asynchronous Voice Note.
     * Uses Reliable Transport under the hood.
     */
    fun recordVoiceNote(targetGroupIdHex: String, sessionKey: ByteArray) {
        logger.logSessionStarted("voice-note", "VoiceNote")
        voiceRecorder.startRecording(
            sessionId = "voice-note-${System.currentTimeMillis()}",
            targetGroupIdHex = targetGroupIdHex,
            sessionKey = sessionKey,
            packetType = VoicePacketType.VOICE_NOTE
        )
    }

    /**
     * Stops recording a Voice Note.
     */
    fun stopVoiceNote() {
        voiceRecorder.stopRecording()
        logger.logSessionEnded("voice-note", "Note finished")
    }
}
