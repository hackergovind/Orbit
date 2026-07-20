package com.bmtp.app.transport

import com.bmtp.app.mesh.MeshTransport
import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketSerializer
import com.bmtp.app.routing.RoutingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors the PacketQueue and schedules packet transmissions based on
 * routing availability and congestion windows.
 */
@Singleton
class PacketScheduler @Inject constructor(
    private val packetQueue: PacketQueue,
    private val offlineQueue: OfflineQueue,
    private val routingEngine: RoutingEngine,
    private val congestionController: CongestionController,
    private val packetSerializer: PacketSerializer,
    private val meshTransport: MeshTransport,
    private val logger: TransportLogger,
    private val stats: TransportStatistics,
    private val deliveryTracker: DeliveryTracker
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var schedulerJob: Job? = null

    fun start() {
        if (schedulerJob?.isActive == true) return
        
        schedulerJob = scope.launch {
            while (isActive) {
                if (packetQueue.isEmpty()) {
                    delay(50) // Short delay when idle
                    continue
                }

                val packet = packetQueue.peek() ?: continue
                val destHex = packet.header.receiverNodeId.joinToString("") { "%02x".format(it) }

                // 1. Check congestion window
                if (!congestionController.canSend(destHex)) {
                    delay(10) // Wait for ACKs to open window
                    continue
                }

                // 2. Lookup route
                val nextHop = routingEngine.getNextHopOrDiscover(packet.header.receiverNodeId)
                if (nextHop == null) {
                    // No active route. Routing engine has started discovery.
                    // We must dequeue and move this to the offline queue so we don't block other destinations
                    val p = packetQueue.dequeue()
                    if (p != null) {
                        offlineQueue.enqueue(destHex, p)
                        val idHex = p.header.packetId.joinToString("") { "%02x".format(it) }
                        deliveryTracker.updateState(idHex, DeliveryState.QUEUED) // It's offline queued
                    }
                    continue
                }

                // 3. We have a route and window is open. Dequeue and transmit.
                val packetToSend = packetQueue.dequeue() ?: continue
                val packetIdHex = packetToSend.header.packetId.joinToString("") { "%02x".format(it) }
                
                try {
                    val bytes = packetSerializer.serialize(packetToSend)
                    meshTransport.sendPacket(nextHop, bytes)
                    
                    stats.recordSent()
                    logger.logSent(packetIdHex)
                    deliveryTracker.updateState(packetIdHex, DeliveryState.SENT)
                    congestionController.onPacketSent(destHex)
                } catch (e: Exception) {
                    logger.logError("Failed to transmit scheduled packet", e)
                }
            }
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }
}
