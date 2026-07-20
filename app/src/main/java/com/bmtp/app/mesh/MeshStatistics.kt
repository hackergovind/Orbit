package com.bmtp.app.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing instantaneous mesh engine metrics.
 */
data class MeshMetrics(
    val packetsReceived: Long = 0,
    val packetsSent: Long = 0,
    val packetsForwarded: Long = 0,
    val packetsDropped: Long = 0,
    val duplicates: Long = 0,
    val ttlExpired: Long = 0,
    val queueSize: Int = 0,
    val neighborCount: Int = 0
)

/**
 * Thread-safe singleton for tracking and exposing mesh network statistics.
 */
@Singleton
class MeshStatistics @Inject constructor() {
    
    private val _metrics = MutableStateFlow(MeshMetrics())
    
    /** Flow of real-time mesh metrics. */
    val metrics: StateFlow<MeshMetrics> = _metrics.asStateFlow()

    fun incrementReceived() {
        _metrics.update { it.copy(packetsReceived = it.packetsReceived + 1) }
    }

    fun incrementSent() {
        _metrics.update { it.copy(packetsSent = it.packetsSent + 1) }
    }

    fun incrementForwarded() {
        _metrics.update { it.copy(packetsForwarded = it.packetsForwarded + 1) }
    }

    fun incrementDropped() {
        _metrics.update { it.copy(packetsDropped = it.packetsDropped + 1) }
    }

    fun incrementDuplicates() {
        _metrics.update { it.copy(duplicates = it.duplicates + 1) }
    }

    fun incrementTtlExpired() {
        _metrics.update { it.copy(ttlExpired = it.ttlExpired + 1) }
    }

    fun updateQueueSize(size: Int) {
        _metrics.update { it.copy(queueSize = size) }
    }

    fun updateNeighborCount(count: Int) {
        _metrics.update { it.copy(neighborCount = count) }
    }
}
