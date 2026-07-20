package com.bmtp.app.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class VoiceState {
    DISCONNECTED, LISTENING, SPEAKING, MUTED, BUFFERING, RECONNECTING, RECORDING
}

data class ParticipantVoiceState(
    val memberIdHex: String,
    val state: VoiceState,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

/**
 * Tracks real-time voice states (Speaking, Muted, etc.) of participants in active sessions.
 */
@Singleton
class VoicePresence @Inject constructor() {
    
    // sessionId -> (memberIdHex -> State)
    private val sessionStates = ConcurrentHashMap<String, ConcurrentHashMap<String, ParticipantVoiceState>>()
    
    private val _presenceFlow = MutableStateFlow<Map<String, Map<String, ParticipantVoiceState>>>(emptyMap())
    val presenceFlow: StateFlow<Map<String, Map<String, ParticipantVoiceState>>> = _presenceFlow.asStateFlow()

    fun updateState(sessionId: String, memberIdHex: String, state: VoiceState) {
        val participants = sessionStates.computeIfAbsent(sessionId) { ConcurrentHashMap() }
        participants[memberIdHex] = ParticipantVoiceState(memberIdHex, state)
        emitUpdate()
    }

    fun getState(sessionId: String, memberIdHex: String): VoiceState {
        return sessionStates[sessionId]?.get(memberIdHex)?.state ?: VoiceState.DISCONNECTED
    }

    fun getSpeakingParticipants(sessionId: String): List<String> {
        val participants = sessionStates[sessionId] ?: return emptyList()
        return participants.filterValues { it.state == VoiceState.SPEAKING }.keys.toList()
    }

    fun clearSession(sessionId: String) {
        sessionStates.remove(sessionId)
        emitUpdate()
    }

    private fun emitUpdate() {
        val snapshot = sessionStates.mapValues { it.value.toMap() }
        _presenceFlow.update { snapshot }
    }
}
