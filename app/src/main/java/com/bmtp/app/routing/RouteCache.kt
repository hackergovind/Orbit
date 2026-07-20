package com.bmtp.app.routing

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LRU Cache for frequently accessed routes to speed up data packet forwarding.
 */
@Singleton
class RouteCache @Inject constructor(
    config: RoutingConfig
) {
    private val lock = ReentrantLock()
    private val cache = object : LinkedHashMap<String, RouteEntry>(config.cacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RouteEntry>?): Boolean {
            return size > config.cacheSize
        }
    }

    fun get(destinationId: String): RouteEntry? = lock.withLock {
        return cache[destinationId]
    }

    fun put(entry: RouteEntry) = lock.withLock {
        cache[entry.destinationId] = entry
    }

    fun remove(destinationId: String) = lock.withLock {
        cache.remove(destinationId)
    }

    fun clear() = lock.withLock {
        cache.clear()
    }
}
