package com.bmtp.app.security_hardening

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates high-level security policies.
 * Evaluates whether an action is permitted based on node reputation and allowlists.
 */
@Singleton
class SecurityPolicy @Inject constructor(
    private val reputationManager: ReputationManager,
    private val allowlistManager: AllowlistManager,
    private val quarantineManager: QuarantineManager
) {

    /**
     * Determines if a packet from this node should be subjected to strict heuristic checks (like Spam Detection).
     * Trusted nodes bypass some checks to save CPU.
     */
    fun requiresStrictHeuristics(nodeId: String): Boolean {
        if (allowlistManager.isAllowlisted(nodeId)) {
            return false // Friends get a free pass on heuristics
        }
        
        // If trust is high, we can relax heuristics to save CPU
        val trust = reputationManager.getTrustScore(nodeId)
        return trust < 0.8f
    }

    /**
     * Checks if this node is allowed to establish a new connection to us.
     * Throws SecurityPolicyException if rejected.
     */
    @Throws(SecurityPolicyException::class)
    fun validateConnectionAttempt(nodeId: String) {
        if (quarantineManager.isBlacklisted(nodeId)) {
            throw SecurityPolicyException("Connection rejected: Node is permanently blacklisted.")
        }
        
        try {
            quarantineManager.checkQuarantineStatus(nodeId)
        } catch (e: QuarantineException) {
            throw SecurityPolicyException("Connection rejected: Node is currently quarantined.", e)
        }
    }
}
