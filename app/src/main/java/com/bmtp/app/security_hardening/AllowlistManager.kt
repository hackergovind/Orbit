package com.bmtp.app.security_hardening

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages an explicit allowlist (e.g., trusted friends).
 * Nodes on this list may bypass certain strict heuristics (like Spam limits) 
 * but are still subject to cryptographic validation and replay detection.
 */
@Singleton
class AllowlistManager @Inject constructor() {
    
    // In a real application, this should be persisted to local storage
    private val allowlistedNodes = ConcurrentHashMap.newKeySet<String>()

    fun allowlist(nodeId: String) {
        allowlistedNodes.add(nodeId)
    }

    fun isAllowlisted(nodeId: String): Boolean {
        return allowlistedNodes.contains(nodeId)
    }
    
    fun removeFromAllowlist(nodeId: String) {
        allowlistedNodes.remove(nodeId)
    }
}
