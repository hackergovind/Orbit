package com.bmtp.transport

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * A shared simulation environment for MockTransports.
 * In a real application, this would act like the "air" that connects various MockTransports together.
 */
object MockEnvironment {
    private val transports = ConcurrentHashMap<String, MockTransport>()

    fun register(transport: MockTransport) {
        transports[transport.nodeId] = transport
    }

    fun unregister(nodeId: String) {
        transports.remove(nodeId)
    }
    
    fun getPeersFor(nodeId: String): List<String> {
        return transports.keys.filter { it != nodeId }
    }

    suspend fun routePacket(fromNodeId: String, toNodeId: String, data: ByteArray): Boolean {
        val destination = transports[toNodeId] ?: return false
        // Simulate network delay
        delay(10)
        destination.receivePacket(fromNodeId, data)
        return true
    }
}

/**
 * A completely memory-based loopback transport for unit testing and simulation.
 * Requires zero hardware dependencies and runs instantaneously on any JVM.
 */
class MockTransport(
    val nodeId: String,
    private val config: TransportConfig = TransportConfig()
) : Transport {

    override val transportId: String = "MOCK_LOOPBACK"
    
    private val _events = MutableSharedFlow<TransportEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    private val _receive = MutableSharedFlow<Pair<String, ByteArray>>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    private var isStarted = false

    override suspend fun start() {
        isStarted = true
        MockEnvironment.register(this)
        _events.tryEmit(TransportEvent.Started)
        
        // Discover existing peers
        MockEnvironment.getPeersFor(nodeId).forEach { peerId ->
            _events.tryEmit(TransportEvent.PeerDiscovered(peerId, "Mock-$peerId", -50))
        }
    }

    override suspend fun stop() {
        isStarted = false
        MockEnvironment.unregister(nodeId)
        _events.tryEmit(TransportEvent.Stopped)
    }

    override suspend fun send(peerId: String, data: ByteArray): Boolean {
        if (!isStarted) return false
        return MockEnvironment.routePacket(fromNodeId = nodeId, toNodeId = peerId, data = data)
    }

    override fun receive(): Flow<Pair<String, ByteArray>> = _receive.asSharedFlow()

    override fun events(): Flow<TransportEvent> = _events.asSharedFlow()

    override suspend fun connect(peerId: String): Boolean {
        if (!isStarted) return false
        // Instantly connects in mock environment
        _events.tryEmit(TransportEvent.PeerConnected(peerId))
        return true
    }

    override suspend fun disconnect(peerId: String) {
        _events.tryEmit(TransportEvent.PeerDisconnected(peerId, "Explicit disconnect"))
    }

    /**
     * Internal hook for MockEnvironment to deliver packets to this instance.
     */
    internal fun receivePacket(fromNodeId: String, data: ByteArray) {
        _receive.tryEmit(Pair(fromNodeId, data))
    }
}
