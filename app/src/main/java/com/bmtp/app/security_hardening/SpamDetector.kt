package com.bmtp.app.security_hardening

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Heuristics-based Spam Detector.
 * Identifies abnormal patterns like a node sending the same payload multiple times
 * or broadcasting excessively beyond normal protocol behavior.
 */
@Singleton
class SpamDetector @Inject constructor(
    private val metrics: SecurityMetrics,
    private val logger: AuditLogger
) {
    // Maps NodeID to their recent payload hashes
    // In a real app, this would use a bounded queue per node to prevent memory leaks
    private val recentPayloads = ConcurrentHashMap<String, MutableList<String>>()
    
    // Thresholds
    private val duplicatePayloadThreshold = 5 // Same payload 5 times recently = SPAM
    private val historySize = 20

    /**
     * Checks if a packet looks like spam.
     * Returns true if spam is detected.
     */
    fun isSpam(nodeId: String, payloadHashHex: String): Boolean {
        val history = recentPayloads.getOrPut(nodeId) { mutableListOf() }
        
        synchronized(history) {
            history.add(payloadHashHex)
            if (history.size > historySize) {
                history.removeAt(0)
            }
            
            // Count occurrences of this specific payload
            val occurrences = history.count { it == payloadHashHex }
            
            if (occurrences >= duplicatePayloadThreshold) {
                metrics.incrementSpamDetections()
                logger.logIncident("SPAM_DETECTED", nodeId, "Duplicate payload sent $occurrences times")
                return true
            }
        }
        
        return false
    }

    /**
     * Clears history for a node (e.g., if they were quarantined and are recovering).
     */
    fun clearHistory(nodeId: String) {
        recentPayloads.remove(nodeId)
    }
}
