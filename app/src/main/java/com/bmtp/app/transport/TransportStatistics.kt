package com.bmtp.app.transport

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class TransportMetrics(
    val packetsQueued: Long = 0,
    val packetsSent: Long = 0,
    val acksReceived: Long = 0,
    val retriesScheduled: Long = 0,
    val packetsExpired: Long = 0,
    val packetsLost: Long = 0,
    val duplicatesDiscarded: Long = 0,
    val offlineQueueStored: Long = 0,
    val offlineQueueTransmitted: Long = 0,
    val deliverySuccessRate: Float = 1.0f,
    val averageLatencyMs: Long = 0,
    val totalLatencySamples: Long = 0,
    val totalLatencyMs: Long = 0
)

@Singleton
class TransportStatistics @Inject constructor() {
    private val _metrics = MutableStateFlow(TransportMetrics())
    val metrics: StateFlow<TransportMetrics> = _metrics.asStateFlow()

    fun recordQueued() = _metrics.update { it.copy(packetsQueued = it.packetsQueued + 1) }
    fun recordSent() = _metrics.update { it.copy(packetsSent = it.packetsSent + 1) }
    fun recordAckReceived() = _metrics.update { 
        val newAcks = it.acksReceived + 1
        val sent = it.packetsSent.coerceAtLeast(1)
        it.copy(
            acksReceived = newAcks,
            deliverySuccessRate = newAcks.toFloat() / sent.toFloat()
        ) 
    }
    fun recordRetry() = _metrics.update { it.copy(retriesScheduled = it.retriesScheduled + 1) }
    fun recordExpired() = _metrics.update { it.copy(packetsExpired = it.packetsExpired + 1) }
    fun recordLost() = _metrics.update { it.copy(packetsLost = it.packetsLost + 1) }
    fun recordDuplicate() = _metrics.update { it.copy(duplicatesDiscarded = it.duplicatesDiscarded + 1) }
    fun recordOfflineStored() = _metrics.update { it.copy(offlineQueueStored = it.offlineQueueStored + 1) }
    fun recordOfflineTransmitted() = _metrics.update { it.copy(offlineQueueTransmitted = it.offlineQueueTransmitted + 1) }
    
    fun recordLatency(latencyMs: Long) = _metrics.update { 
        val totalMs = it.totalLatencyMs + latencyMs
        val samples = it.totalLatencySamples + 1
        it.copy(
            totalLatencyMs = totalMs,
            totalLatencySamples = samples,
            averageLatencyMs = totalMs / samples
        )
    }
}
