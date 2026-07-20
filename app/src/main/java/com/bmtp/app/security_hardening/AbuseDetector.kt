package com.bmtp.app.security_hardening

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates signals of malformed packets and invalid signatures.
 * Interfaces with the ReputationManager to apply penalties.
 */
@Singleton
class AbuseDetector @Inject constructor(
    private val metrics: SecurityMetrics,
    private val logger: AuditLogger,
    private val reputationManager: ReputationManager // Will be created soon
) {
    
    fun reportMalformedPacket(nodeId: String, reason: String) {
        metrics.incrementMalformedPackets()
        logger.logIncident("MALFORMED_PACKET", nodeId, reason)
        reputationManager.applyPenalty(nodeId, PenaltyType.MALFORMED_PACKET)
    }

    fun reportInvalidSignature(nodeId: String) {
        logger.logIncident("INVALID_SIGNATURE", nodeId, "Cryptographic signature validation failed")
        reputationManager.applyPenalty(nodeId, PenaltyType.INVALID_SIGNATURE)
    }

    fun reportProtocolViolation(nodeId: String, rule: String) {
        logger.logIncident("PROTOCOL_VIOLATION", nodeId, rule)
        reputationManager.applyPenalty(nodeId, PenaltyType.MALFORMED_PACKET) // Treat similarly
    }
}
