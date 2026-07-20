package com.bmtp.app.transport

import com.bmtp.app.protocol.Packet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class RetryManager @Inject constructor(
    private val config: TransportConfig,
    private val stats: TransportStatistics,
    private val logger: TransportLogger,
    private val deliveryTracker: DeliveryTracker
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val retryJobs = ConcurrentHashMap<String, Job>()

    /**
     * Optional callback to actually perform the retransmission.
     * This is typically set by the ReliableTransport or PacketScheduler.
     */
    var onRetransmit: ((Packet) -> Unit)? = null

    /**
     * Schedules a packet for automatic retransmission with exponential backoff.
     */
    fun scheduleRetry(packetIdHex: String, packet: Packet) {
        if (retryJobs.containsKey(packetIdHex)) return

        val job = scope.launch {
            var attempt = 1
            var currentTimeout = config.initialAckTimeoutMs

            while (attempt <= config.maxRetries) {
                delay(currentTimeout)
                
                // If the job was cancelled during delay, we're done
                if (!kotlinx.coroutines.isActive) return@launch
                
                logger.logRetryScheduled(packetIdHex, attempt)
                stats.recordRetry()
                deliveryTracker.incrementRetry(packetIdHex)
                
                onRetransmit?.invoke(packet)

                attempt++
                // Exponential backoff up to maxAckTimeoutMs
                val nextTimeout = (currentTimeout * config.backoffMultiplier).toLong()
                currentTimeout = nextTimeout.coerceAtMost(config.maxAckTimeoutMs)
            }
            
            // Max retries exceeded
            logger.logError("Max retries exceeded for packet $packetIdHex", RetryLimitExceededException("Failed after ${config.maxRetries} retries"))
            deliveryTracker.updateState(packetIdHex, DeliveryState.FAILED)
            stats.recordLost()
            retryJobs.remove(packetIdHex)
        }
        
        retryJobs[packetIdHex] = job
    }

    /**
     * Cancels an ongoing retry loop, usually because an ACK was received.
     */
    fun cancelRetry(packetIdHex: String) {
        retryJobs.remove(packetIdHex)?.cancel()
        logger.logRetryCancelled(packetIdHex)
    }
}
