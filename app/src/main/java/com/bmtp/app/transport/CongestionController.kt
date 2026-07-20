package com.bmtp.app.transport

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Basic Congestion Control mechanism.
 * Maintains a Congestion Window (cwnd) per peer.
 * Uses a simple Additive Increase Multiplicative Decrease (AIMD) algorithm.
 */
@Singleton
class CongestionController @Inject constructor(
    private val config: TransportConfig,
    private val logger: TransportLogger
) {
    // Current window size per peer
    private val windows = ConcurrentHashMap<String, AtomicInteger>()
    
    // In-flight packets per peer
    private val inFlight = ConcurrentHashMap<String, AtomicInteger>()

    fun canSend(peerIdHex: String): Boolean {
        val window = windows.computeIfAbsent(peerIdHex) { AtomicInteger(config.congestionWindowInitialSize) }.get()
        val currentInFlight = inFlight.computeIfAbsent(peerIdHex) { AtomicInteger(0) }.get()
        return currentInFlight < window
    }

    fun onPacketSent(peerIdHex: String) {
        inFlight.computeIfAbsent(peerIdHex) { AtomicInteger(0) }.incrementAndGet()
    }

    fun onAckReceived(peerIdHex: String) {
        val flight = inFlight[peerIdHex]
        if (flight != null && flight.get() > 0) {
            flight.decrementAndGet()
        }

        // Additive Increase: Window grows slowly on success
        val window = windows[peerIdHex]
        if (window != null) {
            val current = window.get()
            if (current < config.congestionWindowMaxSize) {
                // Grow by 1 every successful ACK (simplified TCP Reno)
                window.incrementAndGet()
            }
        }
    }

    fun onPacketLoss(peerIdHex: String) {
        val flight = inFlight[peerIdHex]
        if (flight != null && flight.get() > 0) {
            flight.decrementAndGet()
        }

        // Multiplicative Decrease: Window halves on packet loss (timeout)
        val window = windows[peerIdHex]
        if (window != null) {
            var current = window.get()
            current = (current / 2).coerceAtLeast(config.congestionWindowInitialSize / 2)
            current = current.coerceAtLeast(1)
            window.set(current)
            logger.logError("Congestion detected for $peerIdHex. Window reduced to $current", null)
        }
    }
}
