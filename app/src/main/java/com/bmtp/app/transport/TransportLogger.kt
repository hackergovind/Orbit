package com.bmtp.app.transport

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransportLogger @Inject constructor() {
    private val subtag = "ReliableTransport"

    fun logQueued(packetId: String) = LogUtils.d(subtag, "Packet queued: $packetId")
    
    fun logSent(packetId: String) = LogUtils.d(subtag, "Packet sent: $packetId")
    
    fun logAckReceived(packetId: String) = LogUtils.d(subtag, "ACK received: $packetId")
    
    fun logRetryScheduled(packetId: String, attempt: Int) = LogUtils.v(subtag, "Retry scheduled: $packetId (attempt $attempt)")
    
    fun logRetryCancelled(packetId: String) = LogUtils.v(subtag, "Retry cancelled: $packetId")
    
    fun logExpired(packetId: String) = LogUtils.w(subtag, "Packet expired: $packetId")
    
    fun logDuplicateDiscarded(packetId: String) = LogUtils.d(subtag, "Duplicate discarded: $packetId")
    
    fun logOfflineStored(packetId: String) = LogUtils.v(subtag, "Offline queue stored: $packetId")
    
    fun logOfflineTransmitted(packetId: String) = LogUtils.v(subtag, "Offline queue transmitted: $packetId")
    
    fun logError(message: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, message, throwable)
    }
}
