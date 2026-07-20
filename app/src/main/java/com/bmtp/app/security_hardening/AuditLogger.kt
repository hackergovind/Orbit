package com.bmtp.app.security_hardening

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized logger for Security and Governance.
 * Strictly guarantees that no sensitive data (keys, plaintext) is ever logged.
 */
@Singleton
class AuditLogger @Inject constructor() {
    private val subtag = "BMTP-SecAudit"

    fun logIncident(type: String, nodeId: String, details: String) {
        LogUtils.w(subtag, "INCIDENT [$type] | Node: ${maskNodeId(nodeId)} | Details: $details")
    }

    fun logQuarantineEvent(nodeId: String, durationMs: Long, reason: String) {
        LogUtils.w(subtag, "QUARANTINE | Node: ${maskNodeId(nodeId)} | Duration: ${durationMs}ms | Reason: $reason")
    }

    fun logQuarantineLifted(nodeId: String) {
        LogUtils.i(subtag, "QUARANTINE LIFTED | Node: ${maskNodeId(nodeId)}")
    }

    fun logTrustScoreChange(nodeId: String, oldScore: Float, newScore: Float, reason: String) {
        // Only log significant changes to avoid log spam
        if (kotlin.math.abs(oldScore - newScore) > 0.05f) {
            LogUtils.i(subtag, "TRUST_UPDATE | Node: ${maskNodeId(nodeId)} | $oldScore -> $newScore | $reason")
        }
    }

    fun logVersionNegotiation(nodeId: String, versionClaimed: Int, accepted: Boolean) {
        val status = if (accepted) "ACCEPTED" else "REJECTED"
        LogUtils.i(subtag, "VERSION_NEGOTIATION | Node: ${maskNodeId(nodeId)} | Version: $versionClaimed | $status")
    }
    
    fun logSecurityError(message: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, message, throwable)
    }

    /**
     * Partially masks Node IDs (which are often Public Key hashes) so that logs 
     * don't expose full identity information to malicious apps reading logcat.
     */
    private fun maskNodeId(nodeId: String): String {
        if (nodeId.length <= 8) return "***"
        return "${nodeId.take(4)}...${nodeId.takeLast(4)}"
    }
}
