package com.bmtp.app.security_hardening

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Isolates misbehaving nodes temporarily.
 * If a node continues to misbehave after multiple quarantines, they are permanently blacklisted.
 */
@Singleton
class QuarantineManager @Inject constructor(
    private val config: GovernanceConfig,
    private val metrics: SecurityMetrics,
    private val logger: AuditLogger,
    private val blacklistManager: BlacklistManager // Will be implemented next
) {
    // Map of NodeId to Quarantine Expiration Timestamp
    private val quarantinedNodes = ConcurrentHashMap<String, Long>()
    
    // Map of NodeId to Number of times they've been quarantined
    private val quarantineCounts = ConcurrentHashMap<String, Int>()

    /**
     * Places a node in quarantine.
     * Throws QuarantineException if called on an already quarantined node.
     */
    fun quarantineNode(nodeId: String, reason: String) {
        if (isBlacklisted(nodeId)) return // Already dead to us
        
        val count = quarantineCounts.getOrDefault(nodeId, 0) + 1
        quarantineCounts[nodeId] = count
        
        if (count >= 3) {
            // Three strikes, you're out.
            logger.logIncident("PERMANENT_BLACKLIST", nodeId, "Repeated quarantine evasion")
            blacklistManager.blacklist(nodeId)
            quarantinedNodes.remove(nodeId)
            return
        }

        val expiry = System.currentTimeMillis() + config.quarantineDurationMs
        quarantinedNodes[nodeId] = expiry
        
        metrics.incrementQuarantineEvents()
        metrics.updateActiveQuarantines(quarantinedNodes.size)
        
        logger.logQuarantineEvent(nodeId, config.quarantineDurationMs, reason)
    }

    /**
     * Checks if a node is currently permitted to interact with the mesh.
     * If they are in quarantine, checks if the duration has expired.
     * Throws QuarantineException if they are isolated.
     */
    @Throws(QuarantineException::class)
    fun checkQuarantineStatus(nodeId: String) {
        if (isBlacklisted(nodeId)) {
            throw QuarantineException("Node is permanently blacklisted.")
        }
        
        val expiry = quarantinedNodes[nodeId]
        if (expiry != null) {
            if (System.currentTimeMillis() > expiry) {
                // Cooldown finished, lift quarantine
                quarantinedNodes.remove(nodeId)
                metrics.updateActiveQuarantines(quarantinedNodes.size)
                logger.logQuarantineLifted(nodeId)
                // Note: The ReputationManager should ideally reset their score here, 
                // but we keep logic decoupled. IncidentManager coordinates this.
            } else {
                throw QuarantineException("Node is currently in quarantine.")
            }
        }
    }
    
    fun isBlacklisted(nodeId: String): Boolean {
        return blacklistManager.isBlacklisted(nodeId)
    }
}
