package com.bmtp.app.security_hardening

enum class PenaltyType {
    MALFORMED_PACKET,
    INVALID_SIGNATURE,
    REPLAY_DETECTED,
    SPAM_DETECTED
}

enum class RewardType {
    SUCCESSFUL_RELAY
}

/**
 * Represents the ongoing trustworthiness of a node.
 * Value ranges strictly from 0.0f (Malicious/Quarantined) to 1.0f (Perfectly Trusted).
 */
data class TrustScore(
    val nodeId: String,
    val score: Float,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    init {
        require(score in 0.0f..1.0f) { "Trust score must be between 0.0 and 1.0" }
    }
}
