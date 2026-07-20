package com.bmtp.app.routing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the logic for comparing two potential routes to the same destination
 * and deciding which one should be preferred.
 */
@Singleton
class RouteSelector @Inject constructor() {
    
    /**
     * Determines if a new route is better than the existing route.
     *
     * AODV Selection Logic:
     * 1. Prefer higher sequence number.
     * 2. If sequence numbers are equal, prefer lower hop count.
     * 3. If hop counts are also equal, prefer higher score (RSSI/latency).
     *
     * @param existing The currently active route (or null if none exists).
     * @param candidate The potential new route.
     * @return True if the candidate should replace the existing route.
     */
    fun isCandidateBetter(existing: RouteEntry?, candidate: RouteEntry): Boolean {
        if (existing == null) return true
        
        // 1. Sequence Number
        if (candidate.sequenceNumber > existing.sequenceNumber) return true
        if (candidate.sequenceNumber < existing.sequenceNumber) return false
        
        // 2. Hop Count (if SeqNum is equal)
        if (candidate.hopCount < existing.hopCount) return true
        if (candidate.hopCount > existing.hopCount) return false
        
        // 3. Score (if HopCount is equal)
        return candidate.score > existing.score
    }
}
