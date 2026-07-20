package com.bmtp.app.protocol

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import javax.inject.Inject

/**
 * Interface for detecting duplicate packets within the mesh network.
 *
 * Prevents broadcast storms and infinite routing loops by caching the IDs
 * of recently processed packets.
 */
interface PacketCache {
    /**
     * Checks if a packet ID has been seen recently.
     * If not, it adds the ID to the cache.
     *
     * @param packetId The hex string representation of the packet ID.
     * @return True if the packet is a duplicate (already in cache), false if it is new.
     */
    fun isDuplicateOrAdd(packetId: String): Boolean

    /**
     * Clears all entries from the cache.
     */
    fun clear()
}

/**
 * Implementation of [PacketCache] using a thread-safe LRU strategy.
 *
 * @param maxSize The maximum number of packet IDs to store before evicting the oldest.
 * @param expiryMs The time in milliseconds before a packet ID expires from the cache.
 */
class PacketCacheImpl @Inject constructor(
    private val maxSize: Int = ProtocolConstants.CACHE_MAX_SIZE,
    private val expiryMs: Long = ProtocolConstants.CACHE_EXPIRY_MS
) : PacketCache {

    private val lock = ReentrantLock()
    
    // LinkedHashMap with accessOrder = true creates an LRU cache.
    // Key: packetId (String), Value: insertion timestamp (Long)
    private val cache = object : LinkedHashMap<String, Long>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > maxSize
        }
    }

    override fun isDuplicateOrAdd(packetId: String): Boolean {
        val now = System.currentTimeMillis()
        
        lock.withLock {
            val timestamp = cache[packetId]
            
            if (timestamp != null) {
                // If it exists but is expired, treat it as new and update timestamp
                if (now - timestamp > expiryMs) {
                    cache[packetId] = now
                    return false
                }
                return true // Valid duplicate
            }
            
            // New packet, add to cache
            cache[packetId] = now
            return false
        }
    }

    override fun clear() {
        lock.withLock {
            cache.clear()
        }
    }
}
