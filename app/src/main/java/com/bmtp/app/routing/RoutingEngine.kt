package com.bmtp.app.routing

import com.bmtp.app.mesh.MeshTransport
import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketSerializer
import com.bmtp.app.protocol.PacketType
import com.bmtp.app.utils.DeviceIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The core AODV Routing Engine facade.
 * Plugs into the Phase 4 MeshEngine. Intercepts routing control packets and
 * provides next-hop lookups for data packets.
 */
@Singleton
class RoutingEngine @Inject constructor(
    private val routingTable: RoutingTable,
    private val rreqManager: RouteRequestManager,
    private val rrepManager: RouteReplyManager,
    private val rerrManager: RouteErrorManager,
    private val routeDiscovery: RouteDiscovery,
    private val routeExpiryManager: RouteExpiryManager,
    private val seqNumManager: SequenceNumberManager,
    private val config: RoutingConfig,
    private val logger: RoutingLogger,
    private val stats: RoutingStatistics,
    private val packetSerializer: PacketSerializer,
    private val transport: MeshTransport,
    private val routeMetrics: RouteMetrics,
    deviceIdGenerator: DeviceIdGenerator
) {
    private val myDeviceIdBytes = hexStringToByteArray(deviceIdGenerator.getOrCreateDeviceId())
    private val myDeviceIdHex = myDeviceIdBytes.joinToString("") { "%02x".format(it) }
    
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        routeExpiryManager.start()
    }

    /**
     * Finds the next hop for a given destination. If no active route exists,
     * triggers Route Discovery and returns null.
     */
    fun getNextHopOrDiscover(destinationIdBytes: ByteArray): String? {
        val destHex = destinationIdBytes.joinToString("") { "%02x".format(it) }
        val route = routingTable.getActiveRoute(destHex)
        
        if (route != null) {
            return route.nextHopId
        }
        
        // No active route, start discovery
        routeDiscovery.discoverRoute(myDeviceIdBytes, destinationIdBytes, routingTable.getAnyRoute(destHex)?.sequenceNumber ?: 0u)
        return null
    }

    /**
     * Processes incoming routing control packets (RREQ, RREP, RERR).
     * @return True if the packet was a routing packet and consumed. False if it's a normal data packet.
     */
    fun processRoutingPacket(packet: Packet, previousHopId: String): Boolean {
        return when (packet.header.type) {
            PacketType.ROUTE_REQUEST -> {
                handleRreq(packet, previousHopId)
                true
            }
            PacketType.ROUTE_REPLY -> {
                handleRrep(packet, previousHopId)
                true
            }
            PacketType.ERROR -> { // Used for RERR
                handleRerr(packet, previousHopId)
                true
            }
            else -> false
        }
    }

    private fun handleRreq(packet: Packet, previousHopId: String) {
        try {
            val payload = rreqManager.parsePayload(packet.payload)
            val origHex = payload.origId.joinToString("") { "%02x".format(it) }
            val destHex = payload.destId.joinToString("") { "%02x".format(it) }
            
            // Ignore RREQs originated by us
            if (origHex == myDeviceIdHex) return
            
            // 1. Create/Update reverse route back to Originator
            val reverseRoute = RouteEntry(
                destinationId = origHex,
                nextHopId = previousHopId,
                hopCount = (payload.hopCount + 1u).toUByte(),
                sequenceNumber = payload.origSeqNum,
                expiryTimestamp = System.currentTimeMillis() + config.routeLifetimeMs,
                score = routeMetrics.calculateScore((payload.hopCount + 1u).toUByte(), 0, 0)
            )
            routingTable.addOrUpdateRoute(reverseRoute)
            
            // 2. Check if we are the destination
            if (destHex == myDeviceIdHex) {
                // We are the destination, send RREP back
                // Note: Sequence number was incremented when we replied or processed RREQ if needed.
                val rrep = rrepManager.createRrep(myDeviceIdBytes, payload.origId)
                sendUnicast(rrep, previousHopId) // Send back via the previous hop (reverse route)
                return
            }
            
            // 3. Check if we have an active route to the destination
            val activeRoute = routingTable.getActiveRoute(destHex)
            if (activeRoute != null && activeRoute.sequenceNumber >= payload.destSeqNum) {
                // We have a fresh enough route, we can send a gratuitous RREP on behalf of dest
                val gratuitousRrepPayload = RrepPayload(
                    destId = payload.destId,
                    destSeqNum = activeRoute.sequenceNumber,
                    origId = payload.origId,
                    hopCount = activeRoute.hopCount,
                    lifetimeMs = (activeRoute.expiryTimestamp - System.currentTimeMillis()).toUInt()
                )
                val rrep = packetFactory().createRouteReply(myDeviceIdBytes, ByteArray(16), gratuitousRrepPayload.toByteArray())
                sendUnicast(rrep, previousHopId)
                return
            }
            
            // 4. Otherwise, forward the RREQ
            if (payload.hopCount < config.maxHopCount) {
                val forwardedRreq = rreqManager.createForwardedRreq(myDeviceIdBytes, packet, payload)
                broadcast(forwardedRreq)
            }
            
        } catch (e: Exception) {
            logger.logError("Failed to process RREQ", e)
        }
    }

    private fun handleRrep(packet: Packet, previousHopId: String) {
        try {
            val payload = rrepManager.parsePayload(packet.payload)
            val origHex = payload.origId.joinToString("") { "%02x".format(it) }
            val destHex = payload.destId.joinToString("") { "%02x".format(it) }
            
            // 1. Create/Update forward route to Destination
            val forwardRoute = RouteEntry(
                destinationId = destHex,
                nextHopId = previousHopId,
                hopCount = (payload.hopCount + 1u).toUByte(),
                sequenceNumber = payload.destSeqNum,
                expiryTimestamp = System.currentTimeMillis() + payload.lifetimeMs.toLong(),
                score = routeMetrics.calculateScore((payload.hopCount + 1u).toUByte(), 0, 0)
            )
            routingTable.addOrUpdateRoute(forwardRoute)
            
            // 2. If we are the originator, discovery is complete
            if (origHex == myDeviceIdHex) {
                // RouteDiscovery coroutine will pick this up when it polls the routing table
                return
            }
            
            // 3. Otherwise, forward RREP along the reverse path towards the originator
            val reverseRoute = routingTable.getActiveRoute(origHex)
            if (reverseRoute != null) {
                val forwardedRrep = rrepManager.createForwardedRrep(myDeviceIdBytes, payload)
                sendUnicast(forwardedRrep, reverseRoute.nextHopId)
            }
            
        } catch (e: Exception) {
            logger.logError("Failed to process RREP", e)
        }
    }

    private fun handleRerr(packet: Packet, previousHopId: String) {
        try {
            val payload = rerrManager.parsePayload(packet.payload)
            var routesInvalidated = false
            
            for (unreachableDest in payload.destinations) {
                val destHex = unreachableDest.destId.joinToString("") { "%02x".format(it) }
                val currentRoute = routingTable.getActiveRoute(destHex)
                
                // If we use the sender of the RERR as our next hop for this dest, we must invalidate our route too
                if (currentRoute != null && currentRoute.nextHopId == previousHopId) {
                    routingTable.invalidateRoute(destHex)
                    routesInvalidated = true
                }
            }
            
            // If we invalidated any routes, we should forward the RERR so upstream nodes know
            if (routesInvalidated) {
                // Forward the same payload. Real AODV rebuilds the RERR with only routes it actually invalidated.
                broadcast(packet)
            }
            
        } catch (e: Exception) {
            logger.logError("Failed to process RERR", e)
        }
    }

    private fun sendUnicast(packet: Packet, nextHopId: String) {
        scope.launch {
            try {
                val bytes = packetSerializer.serialize(packet)
                transport.sendPacket(nextHopId, bytes)
            } catch (e: Exception) {
                logger.logError("Failed to send unicast routing packet", e)
            }
        }
    }

    private fun broadcast(packet: Packet) {
        scope.launch {
            try {
                val bytes = packetSerializer.serialize(packet)
                transport.sendPacket("BROADCAST", bytes)
            } catch (e: Exception) {
                logger.logError("Failed to broadcast routing packet", e)
            }
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
    
    // Quick hack to get packet factory to avoid cyclical deps or passing it deeply. 
    // Usually injected into engine but since Rreq/Rrep manager needs it, we can just instantiate it here for gratuitous RREP.
    private fun packetFactory() = com.bmtp.app.protocol.PacketFactoryImpl()
}
