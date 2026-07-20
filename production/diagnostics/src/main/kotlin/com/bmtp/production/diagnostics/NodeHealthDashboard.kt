package com.bmtp.production.diagnostics

/**
 * Exposes internal metrics about a Node's health for monitoring systems.
 */
data class NodeHealthDashboard(
    val nodeId: String,
    val uptimeSeconds: Long,
    val activeConnections: Int,
    val packetsSent: Long,
    val packetsReceived: Long,
    val currentMemoryUsageBytes: Long
) {
    fun generateReport(): String {
        return """
            --- NODE HEALTH: $nodeId ---
            Uptime: ${uptimeSeconds}s
            Active Peers: $activeConnections
            Sent/Received: $packetsSent / $packetsReceived
            Memory: ${currentMemoryUsageBytes / 1024 / 1024} MB
            ----------------------------
        """.trimIndent()
    }
}
