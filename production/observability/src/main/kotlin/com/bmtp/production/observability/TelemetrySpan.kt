package com.bmtp.production.observability

import java.util.UUID

/**
 * Represents a distributed tracing span for a packet traveling across the mesh.
 */
data class TelemetrySpan(
    val traceId: String = UUID.randomUUID().toString(),
    val spanId: String = UUID.randomUUID().toString(),
    val parentSpanId: String? = null,
    val operationName: String,
    val startTimeMs: Long = System.currentTimeMillis(),
    var endTimeMs: Long? = null,
    val tags: MutableMap<String, String> = mutableMapOf()
) {
    fun finish() {
        endTimeMs = System.currentTimeMillis()
    }
    
    fun durationMs(): Long? = endTimeMs?.minus(startTimeMs)
}
