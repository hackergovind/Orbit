package com.bmtp.app.transport

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects and suppresses duplicate incoming packets.
 */
@Singleton
class DuplicateDetector @Inject constructor(
    private val config: TransportConfig,
    private val stats: TransportStatistics,
    private val logger: TransportLogger
) {
    private data class CacheEntry(
        val timestamp: Long = System.currentTimeMillis()
    )

    private val lock = ReentrantLock()
    
    // LRU Cache mapping packet ID (Hex String) to CacheEntry
    private val seenPackets = object : LinkedHashMap<String, CacheEntry>(config.duplicateCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > config.duplicateCacheSize
        }
    }

    /**
     * Checks if a packet is a duplicate.
     * @param packetIdHex The hex string of the packet ID.
     * @return true if the packet has been seen recently, false otherwise.
     */
    fun isDuplicate(packetIdHex: String): Boolean = lock.withLock {
        val entry = seenPackets[packetIdHex]
        if (entry != null) {
            val isExpired = (System.currentTimeMillis() - entry.timestamp) > config.duplicateExpiryMs
            if (isExpired) {
                seenPackets.remove(packetIdHex)
                // If it's expired, we treat it as not seen. (Though in reality we shouldn't see it this late)
                seenPackets[packetIdHex] = CacheEntry()
                return false
            } else {
                stats.recordDuplicate()
                logger.logDuplicateDiscarded(packetIdHex)
                return true
            }
        } else {
            seenPackets[packetIdHex] = CacheEntry()
            return false
        }
    }
}
