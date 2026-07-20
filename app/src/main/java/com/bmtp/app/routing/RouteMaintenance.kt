package com.bmtp.app.routing

import com.bmtp.app.mesh.MeshTransport
import com.bmtp.app.protocol.PacketSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors neighbor disconnections and invalidates affected routes.
 * Broadcasts Route Errors (RERR) when routes break.
 */
@Singleton
class RouteMaintenance @Inject constructor(
    private val routingTable: RoutingTable,
    private val rerrManager: RouteErrorManager,
    private val logger: RoutingLogger,
    private val packetSerializer: PacketSerializer,
    private val transport: MeshTransport
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Called when a neighbor disconnects or is deemed unreachable.
     * 
     * @param myId My node ID (needed to originate the RERR).
     * @param lostNeighborId The device ID of the neighbor that disconnected.
     */
    fun onNeighborLost(myId: ByteArray, lostNeighborId: String) {
        scope.launch {
            // Find all active routes that used this neighbor as the next hop
            val affectedRoutes = routingTable.getRoutesUsingNextHop(lostNeighborId)
            
            if (affectedRoutes.isEmpty()) return@launch

            logger.logError("Neighbor $lostNeighborId lost, invalidating ${affectedRoutes.size} routes", null)
            
            val unreachableDests = mutableListOf<UnreachableDestination>()

            for (route in affectedRoutes) {
                // Invalidate it in the table (which increments its sequence number)
                routingTable.invalidateRoute(route.destinationId)
                
                // Get the newly incremented sequence number for the RERR
                val invalidRoute = routingTable.getAnyRoute(route.destinationId)
                if (invalidRoute != null) {
                    val destIdBytes = hexStringToByteArray(route.destinationId)
                    unreachableDests.add(UnreachableDestination(destIdBytes, invalidRoute.sequenceNumber))
                }
            }

            // Generate and broadcast RERR
            if (unreachableDests.isNotEmpty()) {
                val rerrPacket = rerrManager.createRerr(myId, unreachableDests)
                try {
                    val bytes = packetSerializer.serialize(rerrPacket)
                    // Broadcast RERR so upstream nodes know the links are broken
                    transport.sendPacket("BROADCAST", bytes)
                    logger.logRerrSent(affectedRoutes.map { it.destinationId }, "BROADCAST")
                } catch (e: Exception) {
                    logger.logError("Failed to serialize/send RERR", e)
                }
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
}
