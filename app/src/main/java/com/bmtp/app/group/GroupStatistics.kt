package com.bmtp.app.group

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class GroupMetrics(
    val groupCount: Int = 0,
    val activeMembersTotal: Int = 0,
    val messagesSentTotal: Long = 0,
    val broadcastsReceivedTotal: Long = 0,
    val membershipChanges: Long = 0,
    val channelActivityTotal: Long = 0,
    val presenceUpdatesProcessed: Long = 0,
    val syncSuccesses: Long = 0,
    val syncConflictsResolved: Long = 0
)

@Singleton
class GroupStatistics @Inject constructor() {
    private val _metrics = MutableStateFlow(GroupMetrics())
    val metrics: StateFlow<GroupMetrics> = _metrics.asStateFlow()

    fun updateGroupCount(count: Int) = _metrics.update { it.copy(groupCount = count) }
    
    fun updateActiveMembers(count: Int) = _metrics.update { it.copy(activeMembersTotal = count) }
    
    fun recordMessageSent() = _metrics.update { it.copy(messagesSentTotal = it.messagesSentTotal + 1) }
    
    fun recordBroadcastReceived() = _metrics.update { it.copy(broadcastsReceivedTotal = it.broadcastsReceivedTotal + 1) }
    
    fun recordMembershipChange() = _metrics.update { it.copy(membershipChanges = it.membershipChanges + 1) }
    
    fun recordChannelActivity() = _metrics.update { it.copy(channelActivityTotal = it.channelActivityTotal + 1) }
    
    fun recordPresenceUpdate() = _metrics.update { it.copy(presenceUpdatesProcessed = it.presenceUpdatesProcessed + 1) }
    
    fun recordSyncSuccess() = _metrics.update { it.copy(syncSuccesses = it.syncSuccesses + 1) }
    
    fun recordSyncConflict() = _metrics.update { it.copy(syncConflictsResolved = it.syncConflictsResolved + 1) }
}
