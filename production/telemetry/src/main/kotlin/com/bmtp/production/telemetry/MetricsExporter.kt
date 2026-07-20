package com.bmtp.production.telemetry

import com.bmtp.production.diagnostics.NodeHealthDashboard
import kotlinx.coroutines.*

/**
 * Periodically exports aggregated metrics to an external sink.
 */
class MetricsExporter(
    private val healthSource: () -> NodeHealthDashboard,
    private val exportIntervalMs: Long = 60000L
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isActive && isRunning) {
                delay(exportIntervalMs)
                export()
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.cancel()
    }

    private fun export() {
        val snapshot = healthSource()
        // In a real production system, this would HTTP POST to Datadog or Prometheus.
        // For now, we simulate the export.
        println("[Telemetry Export] Payload: ${snapshot.nodeId} | ${snapshot.packetsSent} sent")
    }
}
