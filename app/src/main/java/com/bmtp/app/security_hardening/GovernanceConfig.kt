package com.bmtp.app.security_hardening

/**
 * Configuration parameters for the BMTP Security Hardening & Protocol Governance Engine.
 */
data class GovernanceConfig(
    // Replay Detection
    val replayWindowMs: Long = 5 * 60 * 1000L, // 5 minutes sliding window
    val replayCacheSize: Int = 10000, // Maximum sequence signatures to hold in memory

    // Rate Limiting (Hybrid: Global vs Per Node)
    val globalPacketBurstLimit: Int = 500, // Max total packets per second processed by this device
    val perNodePacketBurstLimit: Int = 50, // Max packets per second from a single neighbor

    // Reputation & Quarantine
    val initialTrustScore: Float = 0.5f, // Untrusted but not penalized
    val quarantineThreshold: Float = 0.2f, // Trust score below this triggers quarantine
    val quarantineDurationMs: Long = 15 * 60 * 1000L, // 15 minutes isolation
    
    // Penalties
    val penaltyMalformedPacket: Float = 0.1f,
    val penaltyInvalidSignature: Float = 0.2f,
    val penaltyReplayDetected: Float = 0.05f,
    val penaltySpamDetected: Float = 0.1f,
    
    // Rewards
    val rewardSuccessfulRelay: Float = 0.01f,
    
    // Versioning
    val supportedProtocolVersions: Set<Int> = setOf(1, 2)
)
