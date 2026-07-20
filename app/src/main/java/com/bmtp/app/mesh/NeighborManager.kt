package com.bmtp.app.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks actively connected neighbors in the mesh network.
 * 
 * Provides a thread-safe list of device IDs that are currently available for
 * packet forwarding.
 */
@Singleton
class NeighborManager @Inject constructor(
    private val logger: MeshLogger,
    private val stats: MeshStatistics
) {
    private val _connectedNeighbors = MutableStateFlow<Set<String>>(emptySet())
    
    /** Flow of currently connected neighbor Device IDs. */
    val connectedNeighbors: StateFlow<Set<String>> = _connectedNeighbors.asStateFlow()

    /**
     * Registers a newly connected neighbor.
     *
     * @param deviceId The ID of the connected device.
     */
    fun onNeighborConnected(deviceId: String) {
        _connectedNeighbors.update { current ->
            if (!current.contains(deviceId)) {
                logger.logNeighborJoined(deviceId)
                val newSet = current + deviceId
                stats.updateNeighborCount(newSet.size)
                newSet
            } else {
                current
            }
        }
    }

    /**
     * Unregisters a disconnected neighbor.
     *
     * @param deviceId The ID of the disconnected device.
     */
    fun onNeighborDisconnected(deviceId: String) {
        _connectedNeighbors.update { current ->
            if (current.contains(deviceId)) {
                logger.logNeighborLeft(deviceId)
                val newSet = current - deviceId
                stats.updateNeighborCount(newSet.size)
                newSet
            } else {
                current
            }
        }
    }
    
    /**
     * Returns true if there is at least one neighbor connected.
     */
    fun hasNeighbors(): Boolean {
        return _connectedNeighbors.value.isNotEmpty()
    }
}
