package com.bmtp.app.performance

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standardized Least-Recently-Used (LRU) Cache wrapper.
 * Prevents OutOfMemoryErrors by capping maximum entries and allowing forced eviction during Memory Pressure.
 */
class BmtpLruCache<K, V>(
    private val maxSize: Int,
    private val onEvict: ((K, V) -> Unit)? = null
) {
    // LinkedHashMap with accessOrder = true enables LRU behavior
    private val map = Collections.synchronizedMap(
        object : java.util.LinkedHashMap<K, V>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
                val shouldEvict = size > maxSize
                if (shouldEvict && eldest != null) {
                    onEvict?.invoke(eldest.key, eldest.value)
                }
                return shouldEvict
            }
        }
    )

    fun put(key: K, value: V) {
        map[key] = value
    }

    fun get(key: K): V? {
        return map[key]
    }

    fun remove(key: K): V? {
        return map.remove(key)
    }

    fun clear() {
        map.clear()
    }
    
    fun size(): Int = map.size
    
    /**
     * Halves the cache size to relieve moderate memory pressure.
     */
    fun trimToHalf() {
        val targetSize = map.size / 2
        var removedCount = 0
        val iterator = map.entries.iterator()
        while (iterator.hasNext() && map.size > targetSize) {
            val entry = iterator.next()
            onEvict?.invoke(entry.key, entry.value)
            iterator.remove()
            removedCount++
        }
    }
}

@Singleton
class CacheManager @Inject constructor(
    private val config: PerformanceConfig
) {
    // Shared caches used across the application
    val packetCache = BmtpLruCache<String, ByteArray>(config.maxCacheEntries)
    val routeCache = BmtpLruCache<String, String>(config.maxCacheEntries)
    val identityCache = BmtpLruCache<String, Any>(config.maxCacheEntries) // E.g., parsed Public Keys
    
    fun evictAll() {
        packetCache.clear()
        routeCache.clear()
        identityCache.clear()
    }

    fun trimToSize() {
        packetCache.trimToHalf()
        routeCache.trimToHalf()
        identityCache.trimToHalf()
    }
}
