package com.bmtp.app.group

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val config: GroupConfig,
    private val logger: GroupLogger,
    private val stats: GroupStatistics
) {
    // In-memory cache of group state
    private val groups = ConcurrentHashMap<String, GroupState>()
    
    // Reactive flow for UI observation
    private val _groupsFlow = MutableStateFlow<List<GroupState>>(emptyList())
    val groupsFlow: StateFlow<List<GroupState>> = _groupsFlow.asStateFlow()

    fun getGroup(groupId: String): GroupState? {
        return groups[groupId]
    }
    
    fun getAllGroups(): List<GroupState> {
        return groups.values.toList()
    }

    fun addGroup(group: GroupState) {
        if (groups.size >= config.maxJoinedGroups) {
            throw SynchronizationException("Max joined groups limit reached (${config.maxJoinedGroups})")
        }
        groups[group.groupId] = group
        emitUpdate()
        stats.updateGroupCount(groups.size)
        logger.logGroupCreated(group.groupId, group.name)
    }

    fun updateGroup(group: GroupState) {
        val existing = groups[group.groupId]
        if (existing == null) {
            addGroup(group)
            return
        }
        
        // Conflict Resolution: Last-Write-Wins (LWW) based on logical timestamp
        if (group.versionTimestamp >= existing.versionTimestamp) {
            groups[group.groupId] = group
            emitUpdate()
        } else {
            stats.recordSyncConflict()
            logger.logError("Rejected stale update for group ${group.groupId}", null)
        }
    }

    fun removeGroup(groupId: String) {
        val removed = groups.remove(groupId)
        if (removed != null) {
            emitUpdate()
            stats.updateGroupCount(groups.size)
            logger.logGroupDeleted(groupId)
        }
    }
    
    private fun emitUpdate() {
        _groupsFlow.update { groups.values.toList() }
    }
    
    fun clear() {
        groups.clear()
        emitUpdate()
        stats.updateGroupCount(0)
    }
}
