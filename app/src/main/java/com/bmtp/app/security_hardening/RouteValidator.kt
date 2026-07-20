package com.bmtp.app.security_hardening

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates Phase 5 routing (AODV) updates.
 * Prevents malicious nodes from manipulating routing tables or creating routing loops.
 */
@Singleton
class RouteValidator @Inject constructor(
    private val abuseDetector: AbuseDetector
) {
    /**
     * Maximum logical hop count for a BMTP Mesh.
     * Anything higher is likely a routing loop or a malicious advertisement.
     */
    private val maxHopCount = 15

    /**
     * Validates a Route Advertisement packet.
     * Throws ProtocolViolationException if invalid.
     */
    @Throws(ProtocolViolationException::class)
    fun validateRouteAdvertisement(
        nodeId: String, 
        advertisedHopCount: Int, 
        sequenceNumber: Int, 
        lastKnownSequence: Int
    ) {
        if (advertisedHopCount <= 0 || advertisedHopCount > maxHopCount) {
            abuseDetector.reportProtocolViolation(nodeId, "Invalid hop count: $advertisedHopCount")
            throw ProtocolViolationException("Route advertisement rejected: impossible hop count.")
        }

        if (sequenceNumber < lastKnownSequence) {
            // A node shouldn't advertise an older sequence number than what we already know.
            // Could be a replay or a desynchronized node, but we reject it safely.
            throw ProtocolViolationException("Route advertisement rejected: stale sequence number.")
        }
    }
}
