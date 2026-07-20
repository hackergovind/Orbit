package com.bmtp.app.security_hardening

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects and rejects duplicate packets to prevent Replay Attacks.
 * Uses a sliding time window (e.g. 5 minutes) and a size-bounded LRU cache to prevent memory exhaustion.
 * Assumes packets older than the window are rejected at the protocol layer via timestamp validation.
 */
@Singleton
class ReplayDetector @Inject constructor(
    private val config: GovernanceConfig,
    private val metrics: SecurityMetrics,
    private val logger: AuditLogger
) {
    // Maps a unique packet signature (or hash) to the timestamp it was received
    // LinkedHashMap with accessOrder = true ensures LRU behavior
    private val signatureCache = Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, Long>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > config.replayCacheSize
            }
        }
    )

    /**
     * Checks if the packet signature has been seen recently.
     * Throws ReplayDetectedException if it has.
     * 
     * @param signatureHex A unique hash of the packet's immutable fields (Sender ID + Sequence + Payload Hash)
     */
    @Throws(ReplayDetectedException::class)
    fun checkAndRemember(nodeId: String, signatureHex: String) {
        val now = System.currentTimeMillis()
        val existingTime = signatureCache[signatureHex]

        if (existingTime != null) {
            // Check if it's within the sliding window
            if (now - existingTime <= config.replayWindowMs) {
                metrics.incrementReplayAttempts()
                logger.logIncident("REPLAY_DETECTED", nodeId, "Duplicate signature: $signatureHex")
                throw ReplayDetectedException("Packet replay detected from node $nodeId")
            }
        }

        // Remember this packet
        signatureCache[signatureHex] = now
        
        // Periodic cleanup of very old entries to save memory (every ~100 checks)
        if (signatureCache.size % 100 == 0) {
            pruneExpiredEntries(now)
        }
    }

    private fun pruneExpiredEntries(now: Long) {
        val threshold = now - config.replayWindowMs
        val iterator = signatureCache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < threshold) {
                iterator.remove()
            } else {
                // Since it's access-ordered, once we hit a new one, we can't guarantee 
                // the rest are new without checking, but it's safe to just let LRU handle the rest
            }
        }
    }
}
