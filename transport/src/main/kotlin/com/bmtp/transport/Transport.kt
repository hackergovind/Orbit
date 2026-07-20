package com.bmtp.transport

import kotlinx.coroutines.flow.Flow

/**
 * Represents the fundamental capabilities of any underlying physical or virtual transport medium.
 * This abstracts away Bluetooth LE, Wi-Fi Direct, TCP, LoRa, etc., from the BMTP core.
 *
 * Architecture Notes:
 * - Implementations must be thread-safe.
 * - Non-blocking I/O is preferred (suspend functions or Flows).
 * - Must not leak platform-specific dependencies into the BMTP Core.
 *
 * Extension Points:
 * - Can be implemented by 3rd-party plugins for Custom Transports (e.g., Audio-based data transfer).
 */
interface Transport {
    
    /**
     * Unique identifier for this transport type (e.g., "BLE", "TCP", "LORA")
     */
    val transportId: String

    /**
     * Starts the transport, beginning discovery/advertising depending on configuration.
     */
    suspend fun start()

    /**
     * Stops the transport, disconnecting all active peers and halting discovery.
     */
    suspend fun stop()

    /**
     * Sends a raw payload to a specific peer over this transport.
     * 
     * @param peerId The physical identifier of the peer (e.g., MAC address, IP).
     * @param data The raw byte array to transmit.
     * @return true if enqueued successfully, false otherwise.
     */
    suspend fun send(peerId: String, data: ByteArray): Boolean

    /**
     * A continuous stream of incoming packets from all peers on this transport.
     * @return Flow emitting Pair(SenderPeerId, Payload)
     */
    fun receive(): Flow<Pair<String, ByteArray>>
    
    /**
     * A continuous stream of lifecycle events (e.g., Connected, Disconnected).
     */
    fun events(): Flow<TransportEvent>

    /**
     * Attempts an explicit connection to a known peer.
     * Some transports (like BLE) require this, others (like UDP) might not.
     */
    suspend fun connect(peerId: String): Boolean

    /**
     * Explicitly disconnects a peer.
     */
    suspend fun disconnect(peerId: String)
}

/**
 * Represents events emitted by a Transport implementation.
 */
sealed class TransportEvent {
    data class PeerDiscovered(val peerId: String, val name: String?, val rssi: Int? = null) : TransportEvent()
    data class PeerConnected(val peerId: String) : TransportEvent()
    data class PeerDisconnected(val peerId: String, val reason: String? = null) : TransportEvent()
    data class Error(val message: String, val cause: Throwable? = null) : TransportEvent()
    object Started : TransportEvent()
    object Stopped : TransportEvent()
}
