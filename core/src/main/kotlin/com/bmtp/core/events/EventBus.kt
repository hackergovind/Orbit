package com.bmtp.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Global Event Bus for the BMTP Core.
 * Allows decoupled components (Routing, Security, Groups, Transport) to communicate asynchronously.
 */
class EventBus {
    private val _events = MutableSharedFlow<BmtpEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<BmtpEvent> = _events.asSharedFlow()

    fun publish(event: BmtpEvent) {
        _events.tryEmit(event)
    }

    /**
     * Utility to filter events of a specific type.
     */
    inline fun <reified T : BmtpEvent> observe(): kotlinx.coroutines.flow.Flow<T> {
        return events.filterIsInstance<T>()
    }
}

/**
 * Base interface for all system events.
 */
interface BmtpEvent

// Core Domain Events
data class PeerDiscoveredEvent(val peerId: String, val name: String?) : BmtpEvent
data class PeerConnectedEvent(val peerId: String) : BmtpEvent
data class PeerDisconnectedEvent(val peerId: String) : BmtpEvent

data class MessageReceivedEvent(val fromNodeId: String, val payload: ByteArray) : BmtpEvent
data class RouteChangedEvent(val destination: String, val nextHop: String, val hops: Int) : BmtpEvent
data class SecurityAlertEvent(val type: String, val nodeId: String, val details: String) : BmtpEvent
