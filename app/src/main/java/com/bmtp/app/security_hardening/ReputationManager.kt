package com.bmtp.app.security_hardening

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Trust Score of known peers.
 * Evaluates penalties and rewards, and triggers Quarantine if scores drop too low.
 */
@Singleton
class ReputationManager @Inject constructor(
    private val config: GovernanceConfig,
    private val quarantineManager: QuarantineManager, // Will be implemented next
    private val metrics: SecurityMetrics,
    private val logger: AuditLogger
) {
    private val trustScores = ConcurrentHashMap<String, TrustScore>()

    fun getTrustScore(nodeId: String): Float {
        return trustScores[nodeId]?.score ?: config.initialTrustScore
    }

    @Synchronized
    fun applyPenalty(nodeId: String, type: PenaltyType) {
        val penalty = when (type) {
            PenaltyType.MALFORMED_PACKET -> config.penaltyMalformedPacket
            PenaltyType.INVALID_SIGNATURE -> config.penaltyInvalidSignature
            PenaltyType.REPLAY_DETECTED -> config.penaltyReplayDetected
            PenaltyType.SPAM_DETECTED -> config.penaltySpamDetected
        }
        adjustScore(nodeId, -penalty, type.name)
    }

    @Synchronized
    fun applyReward(nodeId: String, type: RewardType) {
        val reward = when (type) {
            RewardType.SUCCESSFUL_RELAY -> config.rewardSuccessfulRelay
        }
        adjustScore(nodeId, reward, type.name)
    }

    private fun adjustScore(nodeId: String, delta: Float, reason: String) {
        // Do not adjust if already permanently blacklisted
        if (quarantineManager.isBlacklisted(nodeId)) return
        
        val oldScore = getTrustScore(nodeId)
        val newScore = (oldScore + delta).coerceIn(0.0f, 1.0f)

        trustScores[nodeId] = TrustScore(nodeId, newScore)
        logger.logTrustScoreChange(nodeId, oldScore, newScore, reason)

        updateMetrics()

        if (newScore < config.quarantineThreshold) {
            quarantineManager.quarantineNode(nodeId, "Trust score dropped below threshold ($newScore)")
        }
    }
    
    fun resetScore(nodeId: String) {
        trustScores[nodeId] = TrustScore(nodeId, config.initialTrustScore)
        updateMetrics()
    }

    private fun updateMetrics() {
        if (trustScores.isEmpty()) return
        val sum = trustScores.values.map { it.score }.sum()
        metrics.updateAverageTrust(sum / trustScores.size)
    }
}
