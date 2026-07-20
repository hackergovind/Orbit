package com.bmtp.app.routing

import com.bmtp.app.mesh.MeshTransport
import com.bmtp.app.protocol.PacketSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Route Discovery process.
 * Issues RREQs and handles discovery timeouts and retries.
 */
@Singleton
class RouteDiscovery @Inject constructor(
    private val rreqManager: RouteRequestManager,
    private val routingTable: RoutingTable,
    private val config: RoutingConfig,
    private val logger: RoutingLogger,
    private val stats: RoutingStatistics,
    private val packetSerializer: PacketSerializer,
    private val transport: MeshTransport // Needs to broadcast to all neighbors
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Tracks ongoing discoveries to prevent duplicate concurrent discovery for the same destination
    private val ongoingDiscoveries = ConcurrentHashMap<String, Int>()

    /**
     * Initiates a route discovery for a destination.
     * 
     * @param myId My node ID.
     * @param targetDestId The destination we want to reach.
     * @param knownDestSeqNum The last known sequence number for the destination, or 0.
     */
    fun discoverRoute(myId: ByteArray, targetDestId: ByteArray, knownDestSeqNum: UInt) {
        val destHex = targetDestId.joinToString("") { "%02x".format(it) }
        
        // Prevent concurrent discoveries for the same node
        if (ongoingDiscoveries.containsKey(destHex)) return
        ongoingDiscoveries[destHex] = 0
        
        scope.launch {
            var attempt = 0
            while (attempt < config.discoveryRetryCount) {
                // If a route appeared while we were waiting/retrying, abort discovery
                if (routingTable.getActiveRoute(destHex) != null) {
                    logger.logDiscoverySuccess(destHex, routingTable.getActiveRoute(destHex)!!.hopCount)
                    stats.recordDiscoverySuccess()
                    ongoingDiscoveries.remove(destHex)
                    return@launch
                }
                
                attempt++
                ongoingDiscoveries[destHex] = attempt
                
                val rreq = rreqManager.createRreq(myId, targetDestId, knownDestSeqNum)
                logger.logDiscoveryStarted(destHex, 0u) // Rreq ID is embedded
                
                try {
                    val bytes = packetSerializer.serialize(rreq)
                    stats.recordRreqSent()
                    
                    // Broadcast RREQ to all connected neighbors. 
                    // Phase 4 MeshTransport doesn't have a broadcast method explicitly,
                    // but we can pass a special BROADCAST target or rely on MeshEngine to flood.
                    // For AODV, we want to broadcast to all 1-hop neighbors.
                    transport.sendPacket("BROADCAST", bytes)
                } catch (e: Exception) {
                    logger.logError("Failed to send RREQ", e)
                }
                
                delay(config.discoveryTimeoutMs)
            }
            
            // If we exhaust retries and still no route
            if (routingTable.getActiveRoute(destHex) == null) {
                logger.logDiscoveryFailed(destHex)
                stats.recordDiscoveryFailure()
            } else {
                stats.recordDiscoverySuccess()
            }
            
            ongoingDiscoveries.remove(destHex)
        }
    }
}
