package com.bmtp.app.mesh

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized logging utility for the Mesh Engine.
 * Formats and standardizes mesh event logs.
 */
@Singleton
class MeshLogger @Inject constructor() {
    private val subtag = "MeshEngine"

    fun logReceived(packetId: String, fromDeviceId: String) {
        LogUtils.d(subtag, "Received packet $packetId from node $fromDeviceId")
    }

    fun logForwarded(packetId: String, toNodeCount: Int) {
        LogUtils.d(subtag, "Forwarded packet $packetId to $toNodeCount neighbors")
    }

    fun logDropped(packetId: String, reason: String) {
        LogUtils.w(subtag, "Dropped packet $packetId. Reason: $reason")
    }

    fun logQueued(packetId: String) {
        LogUtils.d(subtag, "Queued packet $packetId for later forwarding")
    }

    fun logDelivered(packetId: String) {
        LogUtils.d(subtag, "Delivered packet $packetId locally")
    }

    fun logNeighborJoined(deviceId: String) {
        LogUtils.i(subtag, "Neighbor joined: $deviceId")
    }

    fun logNeighborLeft(deviceId: String) {
        LogUtils.i(subtag, "Neighbor left: $deviceId")
    }
    
    fun logError(message: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, message, throwable)
    }
}
