package com.bmtp.app.performance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class SystemMetrics(
    val batteryPercent: Float = 1.0f,
    val isCharging: Boolean = false,
    val isBatteryCritical: Boolean = false,
    
    val memoryUsageMb: Long = 0,
    val isMemoryPressureHigh: Boolean = false,
    
    val cpuUsagePercent: Float = 0f,
    
    val packetDeliveryRatio: Float = 1.0f,
    val averageLatencyMs: Float = 0f,
    val activeConnections: Int = 0,
    
    val relayEfficiencyScore: Float = 1.0f, // How useful this node is as a relay (0 to 1)
    val queuedPacketsCount: Int = 0
)

/**
 * Aggregates all performance, system, and mesh metrics into a single reactive flow.
 * Consumed by the Dashboard UI and the Optimization Engines.
 */
@Singleton
class MetricsCollector @Inject constructor(
    private val logger: PerformanceLogger
) {
    private val _metrics = MutableStateFlow(SystemMetrics())
    val metrics: StateFlow<SystemMetrics> = _metrics.asStateFlow()

    fun updateBattery(percent: Float, isCharging: Boolean, isCritical: Boolean) {
        _metrics.update { 
            it.copy(
                batteryPercent = percent,
                isCharging = isCharging,
                isBatteryCritical = isCritical
            )
        }
    }

    fun updateMemory(usageMb: Long, isPressureHigh: Boolean) {
        _metrics.update { 
            it.copy(
                memoryUsageMb = usageMb,
                isMemoryPressureHigh = isPressureHigh
            ) 
        }
    }

    fun updateCpu(usagePercent: Float) {
        _metrics.update { it.copy(cpuUsagePercent = usagePercent) }
    }
    
    fun updateMeshMetrics(
        pdr: Float, 
        latencyMs: Float, 
        connections: Int, 
        queueCount: Int
    ) {
        _metrics.update {
            it.copy(
                packetDeliveryRatio = pdr,
                averageLatencyMs = latencyMs,
                activeConnections = connections,
                queuedPacketsCount = queueCount
            )
        }
    }

    fun updateRelayScore(score: Float) {
        _metrics.update { it.copy(relayEfficiencyScore = score) }
        logger.traceMetric("RelayEfficiencyScore", score)
    }
}
