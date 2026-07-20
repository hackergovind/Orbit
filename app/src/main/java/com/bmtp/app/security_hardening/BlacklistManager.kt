package com.bmtp.app.security_hardening

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the explicit and system-driven permanent blacklist.
 * Traffic from these nodes is dropped at the lowest possible layer.
 */
@Singleton
class BlacklistManager @Inject constructor() {
    
    // In a real application, this should be persisted to local storage (e.g. Room/DataStore)
    private val blacklistedNodes = ConcurrentHashMap.newKeySet<String>()

    fun blacklist(nodeId: String) {
        blacklistedNodes.add(nodeId)
    }

    fun isBlacklisted(nodeId: String): Boolean {
        return blacklistedNodes.contains(nodeId)
    }
    
    fun removeFromBlacklist(nodeId: String) {
        // Administrative override
        blacklistedNodes.remove(nodeId)
    }
}
