package com.bmtp.app.security_hardening

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SecurityTests {

    private lateinit var config: GovernanceConfig
    private lateinit var metrics: SecurityMetrics
    private lateinit var logger: AuditLogger
    private lateinit var blacklistManager: BlacklistManager
    private lateinit var allowlistManager: AllowlistManager
    private lateinit var quarantineManager: QuarantineManager
    private lateinit var reputationManager: ReputationManager
    private lateinit var rateLimiter: RateLimiter
    private lateinit var replayDetector: ReplayDetector
    private lateinit var spamDetector: SpamDetector
    private lateinit var abuseDetector: AbuseDetector
    private lateinit var routeValidator: RouteValidator
    private lateinit var identityValidator: IdentityValidator
    private lateinit var versionNegotiator: VersionNegotiator
    private lateinit var capabilityNegotiator: CapabilityNegotiator
    private lateinit var policy: SecurityPolicy
    private lateinit var incidentManager: IncidentManager

    @Before
    fun setup() {
        config = GovernanceConfig(
            quarantineDurationMs = 1000L, // 1 second for faster tests
            perNodePacketBurstLimit = 5,
            globalPacketBurstLimit = 10
        )
        metrics = SecurityMetrics()
        logger = AuditLogger()
        blacklistManager = BlacklistManager()
        allowlistManager = AllowlistManager()
        
        quarantineManager = QuarantineManager(config, metrics, logger, blacklistManager)
        reputationManager = ReputationManager(config, quarantineManager, metrics, logger)
        
        rateLimiter = RateLimiter(config, metrics, logger)
        replayDetector = ReplayDetector(config, metrics, logger)
        spamDetector = SpamDetector(metrics, logger)
        abuseDetector = AbuseDetector(metrics, logger, reputationManager)
        
        routeValidator = RouteValidator(abuseDetector)
        identityValidator = IdentityValidator(abuseDetector)
        versionNegotiator = VersionNegotiator(config, logger)
        capabilityNegotiator = CapabilityNegotiator()
        
        policy = SecurityPolicy(reputationManager, allowlistManager, quarantineManager)
        
        incidentManager = IncidentManager(
            rateLimiter, replayDetector, spamDetector, abuseDetector,
            routeValidator, identityValidator, versionNegotiator, capabilityNegotiator,
            reputationManager, quarantineManager, policy, metrics, logger
        )
    }

    @Test
    fun `Replay Attack - Rejects duplicate packet signatures`() {
        val nodeId = "NodeA"
        val signature = "abcdef123456"
        
        // First packet should succeed
        incidentManager.secureIncomingPacket(nodeId, signature, "payload1")
        
        // Second packet with same signature should fail
        assertThrows(ReplayDetectedException::class.java) {
            incidentManager.secureIncomingPacket(nodeId, signature, "payload2")
        }
        
        assertEquals(1, metrics.state.value.totalReplayAttempts)
    }

    @Test
    fun `Rate Limiting - Throws when burst limit exceeded`() {
        val nodeId = "NodeB"
        
        // Config limit is 5. So 5 should pass.
        for (i in 1..5) {
            incidentManager.secureIncomingPacket(nodeId, "sig$i", "payload$i")
        }
        
        // The 6th should throw RateLimitExceededException
        assertThrows(RateLimitExceededException::class.java) {
            incidentManager.secureIncomingPacket(nodeId, "sig6", "payload6")
        }
        
        assertEquals(1, metrics.state.value.totalRateLimitEvents)
    }

    @Test
    fun `Quarantine Lifecycle - Abuse leads to quarantine and recovery`() {
        val nodeId = "NodeC"
        
        // Repeated malformed packets to drain trust
        for (i in 1..6) {
            abuseDetector.reportMalformedPacket(nodeId, "Test malformed")
        }
        
        // Trust should be low enough to trigger quarantine
        val score = reputationManager.getTrustScore(nodeId)
        assertTrue("Score $score should be below threshold ${config.quarantineThreshold}", score < config.quarantineThreshold)
        
        // Further packets should be rejected with QuarantineException
        assertThrows(SecurityPolicyException::class.java) {
            incidentManager.secureIncomingPacket(nodeId, "sig_new", "payload")
        }
        
        // Wait for cooldown
        Thread.sleep(1100L)
        
        // Should be able to send again
        incidentManager.secureIncomingPacket(nodeId, "sig_recovered", "payload")
    }

    @Test
    fun `Version Negotiation - Rejects unsupported versions`() {
        val nodeId = "NodeD"
        val validKey = ByteArray(32) // ED25519 length
        
        // Peer only supports version 99 (we support 1, 2)
        assertThrows(VersionMismatchException::class.java) {
            incidentManager.secureHandshake(nodeId, validKey, setOf(99), setOf())
        }
    }
}
