package com.bmtp.app.routing

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe storage for AODV route entries.
 */
@Singleton
class RoutingTable @Inject constructor(
    private val selector: RouteSelector,
    private val logger: RoutingLogger,
    private val stats: RoutingStatistics,
    private val config: RoutingConfig
) {
    private val lock = ReentrantReadWriteLock()
    
    // Key: Destination Node ID (Hex String)
    private val routes = mutableMapOf<String, RouteEntry>()

    /**
     * Looks up an active route for the given destination.
     *
     * @param destinationId The target node ID.
     * @return The [RouteEntry] if an active one exists, otherwise null.
     */
    fun getActiveRoute(destinationId: String): RouteEntry? = lock.read {
        val entry = routes[destinationId]
        if (entry != null && entry.status == RouteStatus.ACTIVE) {
            return entry
        }
        return null
    }
    
    /**
     * Looks up any route (Active or Invalid) for the given destination.
     * Useful for checking sequence numbers of broken routes.
     */
    fun getAnyRoute(destinationId: String): RouteEntry? = lock.read {
        return routes[destinationId]
    }

    /**
     * Updates the routing table with a candidate route.
     * The candidate is only added if it is better than the existing route.
     *
     * @param candidate The new route entry to evaluate.
     * @return True if the route was added/updated, false if rejected.
     */
    fun addOrUpdateRoute(candidate: RouteEntry): Boolean = lock.write {
        val existing = routes[candidate.destinationId]
        
        if (selector.isCandidateBetter(existing, candidate)) {
            val isNew = existing == null
            routes[candidate.destinationId] = candidate
            
            if (isNew) {
                stats.recordRouteCreated()
                stats.updateActiveRoutesCount(activeRouteCountUnsafe())
            }
            logger.logRouteCreated(candidate.destinationId, candidate.nextHopId, candidate.hopCount)
            return true
        }
        return false
    }

    /**
     * Marks a route as invalid (e.g., due to link failure).
     * The route is kept in the table so its sequence number is remembered.
     *
     * @param destinationId The target node ID.
     */
    fun invalidateRoute(destinationId: String) = lock.write {
        val entry = routes[destinationId]
        if (entry != null && entry.status == RouteStatus.ACTIVE) {
            // Sequence numbers must be incremented when invalidating a route in AODV
            val newEntry = entry.copy(
                status = RouteStatus.INVALID,
                sequenceNumber = entry.sequenceNumber + 1u
            )
            routes[destinationId] = newEntry
            
            stats.recordRouteDestroyed()
            stats.updateActiveRoutesCount(activeRouteCountUnsafe())
            logger.logRouteRemoved(destinationId, "Link broken / Invalidated")
        }
    }

    /**
     * Retrieves all routes currently using a specific next hop.
     * Useful when a neighbor disconnects and those routes need invalidation.
     */
    fun getRoutesUsingNextHop(nextHopId: String): List<RouteEntry> = lock.read {
        return routes.values.filter { it.nextHopId == nextHopId && it.status == RouteStatus.ACTIVE }
    }

    /**
     * Evicts expired routes from the table.
     * Called periodically by the RouteExpiryManager.
     */
    fun evictExpiredRoutes(currentTimeMs: Long) = lock.write {
        val toRemove = routes.filterValues { 
            it.status == RouteStatus.INVALID || it.expiryTimestamp < currentTimeMs 
        }.keys

        for (key in toRemove) {
            routes.remove(key)
            logger.logRouteRemoved(key, "Expired")
        }
        
        stats.updateActiveRoutesCount(activeRouteCountUnsafe())
    }
    
    private fun activeRouteCountUnsafe(): Int {
        return routes.values.count { it.status == RouteStatus.ACTIVE }
    }
}
