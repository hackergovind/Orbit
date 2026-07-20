package com.bmtp.app.security_hardening

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodically checks the status of neighbors to detect unresponsive or silently failing nodes.
 * E.g., nodes that accept packets but never route them further (Blackhole attack).
 */
@Singleton
class NodeHealthMonitor @Inject constructor(
    private val reputationManager: ReputationManager,
    private val logger: AuditLogger
) {
    // NodeId to the count of packets we sent them that they failed to acknowledge
    private val unacknowledgedPackets = ConcurrentHashMap<String, Int>()
    
    // Threshold before we consider a node unhealthy
    private val unacknowledgedThreshold = 10

    fun recordSentPacket(nodeId: String) {
        val current = unacknowledgedPackets.getOrDefault(nodeId, 0)
        unacknowledgedPackets[nodeId] = current + 1
        
        if (current + 1 >= unacknowledgedThreshold) {
            logger.logIncident("NODE_UNRESPONSIVE", nodeId, "Failed to acknowledge $unacknowledgedThreshold consecutive packets")
            // Apply a minor penalty for being a blackhole. 
            // If they do this repeatedly, they will be quarantined.
            reputationManager.applyPenalty(nodeId, PenaltyType.MALFORMED_PACKET) // Reusing small penalty
            
            // Reset counter to give them a chance to recover without getting infinitely penalized
            unacknowledgedPackets[nodeId] = 0
        }
    }

    fun recordAcknowledgedPacket(nodeId: String) {
        // They are alive and healthy
        unacknowledgedPackets[nodeId] = 0
        reputationManager.applyReward(nodeId, RewardType.SUCCESSFUL_RELAY)
    }
}
