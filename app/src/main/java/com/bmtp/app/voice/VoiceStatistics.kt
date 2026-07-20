package com.bmtp.app.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class VoiceMetrics(
    val activeSessionsCount: Int = 0,
    val totalPacketsSent: Long = 0,
    val totalPacketsReceived: Long = 0,
    val totalPacketsDropped: Long = 0, // Due to jitter/late
    val jitterBufferUnderruns: Long = 0,
    val jitterBufferOverflows: Long = 0,
    val totalBytesTransmitted: Long = 0,
    
    /** Current estimated Mean Opinion Score (1.0 to 5.0) based on packet loss and jitter */
    val estimatedMos: Float = 5.0f,
    
    /** Current average latency in milliseconds */
    val averageLatencyMs: Float = 0f,
    
    /** Current estimated packet loss rate (0.0 to 1.0) */
    val packetLossRate: Float = 0f
)

@Singleton
class VoiceStatistics @Inject constructor() {
    private val _metrics = MutableStateFlow(VoiceMetrics())
    val metrics: StateFlow<VoiceMetrics> = _metrics.asStateFlow()

    fun updateActiveSessions(count: Int) = _metrics.update { it.copy(activeSessionsCount = count) }
    
    fun recordPacketSent(sizeBytes: Int) = _metrics.update { 
        it.copy(
            totalPacketsSent = it.totalPacketsSent + 1,
            totalBytesTransmitted = it.totalBytesTransmitted + sizeBytes
        ) 
    }
    
    fun recordPacketReceived(sizeBytes: Int) = _metrics.update { 
        it.copy(
            totalPacketsReceived = it.totalPacketsReceived + 1,
            totalBytesTransmitted = it.totalBytesTransmitted + sizeBytes
        ) 
    }
    
    fun recordPacketDropped() = _metrics.update { it.copy(totalPacketsDropped = it.totalPacketsDropped + 1) }
    
    fun recordJitterUnderrun() = _metrics.update { it.copy(jitterBufferUnderruns = it.jitterBufferUnderruns + 1) }
    
    fun recordJitterOverflow() = _metrics.update { it.copy(jitterBufferOverflows = it.jitterBufferOverflows + 1) }
    
    fun updateNetworkQuality(latencyMs: Float, lossRate: Float) {
        _metrics.update {
            val mos = calculateMos(latencyMs, lossRate)
            it.copy(
                averageLatencyMs = latencyMs,
                packetLossRate = lossRate,
                estimatedMos = mos
            )
        }
    }
    
    /**
     * Simplified E-model estimation for Mean Opinion Score (MOS) based on latency and loss.
     * Scale: 1 (Bad) to 5 (Excellent).
     */
    private fun calculateMos(latencyMs: Float, lossRate: Float): Float {
        // Base R-factor
        var r = 93.2f
        
        // Latency penalty
        val effectiveLatency = latencyMs + 20f // Add processing buffer estimate
        if (effectiveLatency > 177.3f) {
            r -= (effectiveLatency - 177.3f) / 10f
        }
        
        // Packet loss penalty (exponential impact)
        r -= (lossRate * 100f) * 2.5f
        
        r = r.coerceIn(0f, 100f)
        
        // Convert R-factor to MOS
        val mos = 1f + (0.035f * r) + (r * (r - 60f) * (100f - r) * 7.0e-6f)
        return mos.coerceIn(1f, 5f)
    }
}
