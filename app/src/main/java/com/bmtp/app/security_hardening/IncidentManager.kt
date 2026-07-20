package com.bmtp.app.security_hardening

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrator for all incoming packets.
 * Runs them through the gauntlet of security checks: Rate Limits -> Replay -> Blacklist -> Validation -> Spam.
 */
@Singleton
class IncidentManager @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val replayDetector: ReplayDetector,
    private val spamDetector: SpamDetector,
    private val abuseDetector: AbuseDetector,
    private val routeValidator: RouteValidator,
    private val identityValidator: IdentityValidator,
    private val versionNegotiator: VersionNegotiator,
    private val capabilityNegotiator: CapabilityNegotiator,
    private val reputationManager: ReputationManager,
    private val quarantineManager: QuarantineManager,
    private val policy: SecurityPolicy,
    private val metrics: SecurityMetrics,
    private val logger: AuditLogger
) {

    /**
     * Secures a newly established connection (Handshake phase).
     * Validates identity, negotiates version, and ensures the peer is not quarantined.
     */
    @Throws(SecurityException::class)
    fun secureHandshake(
        nodeId: String, 
        publicKey: ByteArray, 
        peerVersions: Set<Int>,
        peerCapabilities: Set<BmtpCapability>
    ): Pair<Int, Set<BmtpCapability>> {
        
        // 1. Is this node allowed to connect?
        policy.validateConnectionAttempt(nodeId)
        
        // 2. Validate cryptographic identity
        identityValidator.validatePublicKey(nodeId, publicKey)
        
        // 3. Negotiate Version
        val negotiatedVersion = versionNegotiator.negotiate(nodeId, peerVersions)
        
        // 4. Determine mutual capabilities
        val mutuallySupported = capabilityNegotiator.getMutuallySupported(peerCapabilities)
        
        return Pair(negotiatedVersion, mutuallySupported)
    }

    /**
     * Secures an incoming packet.
     * Throws a specific SecurityException if the packet must be dropped.
     */
    @Throws(SecurityException::class)
    fun secureIncomingPacket(nodeId: String, packetSignatureHex: String, payloadHashHex: String) {
        
        // 1. Are they quarantined right now?
        quarantineManager.checkQuarantineStatus(nodeId)

        // 2. Rate Limiting (Will throw if exceeded)
        rateLimiter.consumeToken(nodeId)

        // 3. Replay Detection (Will throw if duplicate)
        try {
            replayDetector.checkAndRemember(nodeId, packetSignatureHex)
        } catch (e: ReplayDetectedException) {
            reputationManager.applyPenalty(nodeId, PenaltyType.REPLAY_DETECTED)
            throw e
        }

        // 4. Heuristic Spam Check (Only if policy demands it)
        if (policy.requiresStrictHeuristics(nodeId)) {
            if (spamDetector.isSpam(nodeId, payloadHashHex)) {
                reputationManager.applyPenalty(nodeId, PenaltyType.SPAM_DETECTED)
                throw SecurityPolicyException("Packet rejected: Spam detected from $nodeId")
            }
        }
    }
    
    /**
     * Validates a routing advertisement packet.
     */
    @Throws(SecurityException::class)
    fun secureRouteAdvertisement(
        nodeId: String, 
        advertisedHopCount: Int, 
        sequenceNumber: Int, 
        lastKnownSequence: Int
    ) {
        quarantineManager.checkQuarantineStatus(nodeId)
        routeValidator.validateRouteAdvertisement(nodeId, advertisedHopCount, sequenceNumber, lastKnownSequence)
    }
}
