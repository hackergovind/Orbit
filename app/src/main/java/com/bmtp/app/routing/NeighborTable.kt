package com.bmtp.app.routing

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Information about a connected neighbor.
 */
data class NeighborInfo(
    val deviceId: String,
    val rssi: Int,
    val lastSeenTimestamp: Long
)

/**
 * Thread-safe table storing information about directly connected neighbors.
 */
@Singleton
class NeighborTable @Inject constructor() {
    private val neighbors = ConcurrentHashMap<String, NeighborInfo>()

    fun addOrUpdateNeighbor(deviceId: String, rssi: Int = 0) {
        neighbors[deviceId] = NeighborInfo(deviceId, rssi, System.currentTimeMillis())
    }

    fun removeNeighbor(deviceId: String) {
        neighbors.remove(deviceId)
    }

    fun getNeighbor(deviceId: String): NeighborInfo? {
        return neighbors[deviceId]
    }

    fun getAllNeighbors(): List<NeighborInfo> {
        return neighbors.values.toList()
    }
    
    fun hasNeighbor(deviceId: String): Boolean {
        return neighbors.containsKey(deviceId)
    }
}
