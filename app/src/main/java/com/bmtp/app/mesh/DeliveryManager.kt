package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles packets that are addressed to this specific node or are broadcast.
 * Decouples the mesh engine from the application/UI layer.
 */
@Singleton
class DeliveryManager @Inject constructor(
    private val logger: MeshLogger
) {
    private val _incomingDeliveries = MutableSharedFlow<Packet>(extraBufferCapacity = 100)
    
    /**
     * Flow of packets successfully delivered to this node.
     * The application layer (e.g., chat repository) should collect from this flow.
     */
    val incomingDeliveries: SharedFlow<Packet> = _incomingDeliveries.asSharedFlow()

    /**
     * Delivers a packet locally.
     *
     * @param packet The validated packet intended for this node.
     */
    fun deliverLocally(packet: Packet) {
        logger.logDelivered(packet.header.packetIdAsString())
        val success = _incomingDeliveries.tryEmit(packet)
        if (!success) {
            logger.logError("Delivery flow buffer overflow, dropped packet locally: ${packet.header.packetIdAsString()}")
        }
    }
}
