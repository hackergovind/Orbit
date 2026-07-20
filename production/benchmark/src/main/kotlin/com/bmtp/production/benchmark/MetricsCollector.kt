package com.bmtp.production.benchmark

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Aggregates runtime telemetry and performance metrics from the benchmark runner.
 */
class MetricsCollector {
    private val latencySamples = mutableListOf<Long>()
    private val throughputBytesPerSec = AtomicLong(0)
    private val totalPacketsDelivered = AtomicLong(0)
    private val totalPacketsDropped = AtomicLong(0)

    fun recordLatency(latencyMs: Long) {
        synchronized(latencySamples) {
            latencySamples.add(latencyMs)
        }
    }

    fun recordDelivery(sizeBytes: Int) {
        totalPacketsDelivered.incrementAndGet()
        throughputBytesPerSec.addAndGet(sizeBytes.toLong())
    }

    fun recordDrop() {
        totalPacketsDropped.incrementAndGet()
    }

    /**
     * Calculates the Delivery Ratio (0.0 to 1.0)
     */
    fun getDeliveryRatio(): Double {
        val total = totalPacketsDelivered.get() + totalPacketsDropped.get()
        if (total == 0L) return 0.0
        return totalPacketsDelivered.get().toDouble() / total.toDouble()
    }

    /**
     * Calculates Average Latency in milliseconds.
     */
    fun getAverageLatency(): Double {
        synchronized(latencySamples) {
            if (latencySamples.isEmpty()) return 0.0
            return latencySamples.average()
        }
    }
    
    fun getTotalDelivered() = totalPacketsDelivered.get()
}
