package com.bmtp.app.transport

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class DeliveryState {
    QUEUED, SENT, ACKNOWLEDGED, DELIVERED, EXPIRED, DROPPED, FAILED
}

data class PacketDeliveryInfo(
    val packetIdHex: String,
    val destIdHex: String,
    var state: DeliveryState,
    val queuedAt: Long = System.currentTimeMillis(),
    var sentAt: Long = 0L,
    var ackedAt: Long = 0L,
    var retryCount: Int = 0
)

@Singleton
class DeliveryTracker @Inject constructor(
    private val logger: TransportLogger
) {
    private val tracker = ConcurrentHashMap<String, PacketDeliveryInfo>()

    fun track(packetIdHex: String, destIdHex: String) {
        tracker[packetIdHex] = PacketDeliveryInfo(packetIdHex, destIdHex, DeliveryState.QUEUED)
    }

    fun updateState(packetIdHex: String, state: DeliveryState) {
        val info = tracker[packetIdHex]
        if (info != null) {
            info.state = state
            when (state) {
                DeliveryState.SENT -> info.sentAt = System.currentTimeMillis()
                DeliveryState.ACKNOWLEDGED -> info.ackedAt = System.currentTimeMillis()
                else -> {}
            }
        }
    }

    fun incrementRetry(packetIdHex: String) {
        val info = tracker[packetIdHex]
        if (info != null) {
            info.retryCount++
        }
    }

    fun getInfo(packetIdHex: String): PacketDeliveryInfo? = tracker[packetIdHex]
    
    fun removeTracker(packetIdHex: String) {
        tracker.remove(packetIdHex)
    }
}
