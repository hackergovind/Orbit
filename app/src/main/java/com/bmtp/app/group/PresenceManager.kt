package com.bmtp.app.group

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class PresenceStatus {
    ONLINE, OFFLINE, BUSY, AWAY, INVISIBLE, TYPING, RECORDING
}

data class MemberPresence(
    val memberIdHex: String,
    val status: PresenceStatus,
    val lastSeenAt: Long = System.currentTimeMillis()
)

/**
 * Tracks the presence (Online, Offline, Typing) of members within groups.
 */
@Singleton
class PresenceManager @Inject constructor(
    private val config: GroupConfig,
    private val stats: GroupStatistics
) {
    // Map of GroupId -> (Map of MemberIdHex -> Presence)
    private val presenceCache = ConcurrentHashMap<String, ConcurrentHashMap<String, MemberPresence>>()
    
    private val _presenceFlow = MutableStateFlow<Map<String, Map<String, MemberPresence>>>(emptyMap())
    val presenceFlow: StateFlow<Map<String, Map<String, MemberPresence>>> = _presenceFlow.asStateFlow()

    fun updatePresence(groupId: String, memberIdHex: String, status: PresenceStatus) {
        val groupPresence = presenceCache.computeIfAbsent(groupId) { ConcurrentHashMap() }
        
        // If invisible, we just mark as offline for others
        val effectiveStatus = if (status == PresenceStatus.INVISIBLE) PresenceStatus.OFFLINE else status
        
        val newPresence = MemberPresence(memberIdHex, effectiveStatus)
        groupPresence[memberIdHex] = newPresence
        
        stats.recordPresenceUpdate()
        emitUpdate()
    }

    fun getPresence(groupId: String, memberIdHex: String): PresenceStatus {
        val presence = presenceCache[groupId]?.get(memberIdHex) ?: return PresenceStatus.OFFLINE
        
        // Check if heartbeat expired
        val age = System.currentTimeMillis() - presence.lastSeenAt
        if (age > config.presenceOfflineThresholdMs) {
            return PresenceStatus.OFFLINE
        }
        
        return presence.status
    }

    /**
     * Cleans up stale presence entries to free memory.
     * Called periodically by GroupService.
     */
    fun evictStalePresence() {
        val now = System.currentTimeMillis()
        var changed = false
        
        presenceCache.forEach { (_, groupPresence) ->
            val it = groupPresence.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                if (now - entry.value.lastSeenAt > config.presenceOfflineThresholdMs) {
                    it.remove()
                    changed = true
                }
            }
        }
        
        if (changed) {
            emitUpdate()
        }
    }
    
    private fun emitUpdate() {
        // Deep copy for the flow to avoid concurrent modification issues downstream
        val snapshot = presenceCache.mapValues { it.value.toMap() }
        _presenceFlow.update { snapshot }
    }
}
