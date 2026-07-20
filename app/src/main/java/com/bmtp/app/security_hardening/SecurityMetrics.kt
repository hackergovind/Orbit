package com.bmtp.app.security_hardening

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class SecurityState(
    val totalPacketsRejected: Long = 0,
    val totalReplayAttempts: Long = 0,
    val totalRateLimitEvents: Long = 0,
    val totalSpamDetections: Long = 0,
    val totalMalformedPackets: Long = 0,
    val totalQuarantineEvents: Long = 0,
    
    // Number of nodes currently in Quarantine
    val activeQuarantines: Int = 0,
    
    // Average trust score of all known nodes (0.0 to 1.0)
    val averageNetworkTrust: Float = 0.5f
)

/**
 * Real-time tracker for security and protocol governance events.
 */
@Singleton
class SecurityMetrics @Inject constructor() {
    private val _state = MutableStateFlow(SecurityState())
    val state: StateFlow<SecurityState> = _state.asStateFlow()

    fun incrementRejectedPackets() {
        _state.update { it.copy(totalPacketsRejected = it.totalPacketsRejected + 1) }
    }

    fun incrementReplayAttempts() {
        _state.update { it.copy(totalReplayAttempts = it.totalReplayAttempts + 1) }
    }

    fun incrementRateLimitEvents() {
        _state.update { it.copy(totalRateLimitEvents = it.totalRateLimitEvents + 1) }
    }

    fun incrementSpamDetections() {
        _state.update { it.copy(totalSpamDetections = it.totalSpamDetections + 1) }
    }
    
    fun incrementMalformedPackets() {
        _state.update { it.copy(totalMalformedPackets = it.totalMalformedPackets + 1) }
    }

    fun incrementQuarantineEvents() {
        _state.update { it.copy(totalQuarantineEvents = it.totalQuarantineEvents + 1) }
    }
    
    fun updateActiveQuarantines(count: Int) {
        _state.update { it.copy(activeQuarantines = count) }
    }

    fun updateAverageTrust(average: Float) {
        _state.update { it.copy(averageNetworkTrust = average) }
    }
}
