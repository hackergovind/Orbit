package com.bmtp.app.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodically sweeps systems for expired objects:
 * - Evicts stale buffered out-of-order packets.
 * - Evicts expired offline queue packets.
 */
@Singleton
class TimeoutManager @Inject constructor(
    private val config: TransportConfig,
    private val sequenceManager: SequenceManager,
    private val offlineQueue: OfflineQueue,
    private val logger: TransportLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var sweepJob: Job? = null

    fun start() {
        if (sweepJob?.isActive == true) return
        
        sweepJob = scope.launch {
            while (isActive) {
                delay(config.timeoutSweepIntervalMs)
                try {
                    sequenceManager.evictStaleBufferedPackets()
                    offlineQueue.removeExpired()
                } catch (e: Exception) {
                    logger.logError("Error during timeout sweep", e)
                }
            }
        }
    }

    fun stop() {
        sweepJob?.cancel()
        sweepJob = null
    }
}
