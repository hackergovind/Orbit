package com.bmtp.app.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages adaptive bitrates based on network conditions reported by VoiceStatistics.
 */
@Singleton
class VoiceScheduler @Inject constructor(
    private val config: VoiceConfig,
    private val stats: VoiceStatistics,
    private val logger: VoiceLogger
) {
    private val _currentBitrateBps = MutableStateFlow(config.baseBitrateBps)
    val currentBitrateBps: StateFlow<Int> = _currentBitrateBps.asStateFlow()

    /**
     * Evaluates current network metrics and adapts the target encoder bitrate.
     * Call this periodically (e.g., every 5 seconds) during an active session.
     */
    fun evaluateNetworkConditions() {
        val currentMetrics = stats.metrics.value
        val oldBitrate = _currentBitrateBps.value
        var newBitrate = oldBitrate

        // High packet loss -> drastically reduce bitrate to save bandwidth and reduce congestion
        if (currentMetrics.packetLossRate > 0.15f) { // > 15% loss
            newBitrate = (oldBitrate * 0.7).toInt().coerceAtLeast(config.minBitrateBps)
        } 
        // Moderate packet loss
        else if (currentMetrics.packetLossRate > 0.05f) { // 5% - 15%
            newBitrate = (oldBitrate * 0.9).toInt().coerceAtLeast(config.minBitrateBps)
        }
        // Excellent conditions -> slowly increase bitrate back to max
        else if (currentMetrics.packetLossRate < 0.01f && currentMetrics.averageLatencyMs < 100f) {
            newBitrate = (oldBitrate * 1.1).toInt().coerceAtMost(config.maxBitrateBps)
        }

        if (newBitrate != oldBitrate) {
            _currentBitrateBps.value = newBitrate
            logger.logBitrateChanged(oldBitrate, newBitrate, "Adaptive adjustment (Loss: ${currentMetrics.packetLossRate})")
        }
    }
    
    fun resetToDefault() {
        _currentBitrateBps.value = config.baseBitrateBps
    }
}
