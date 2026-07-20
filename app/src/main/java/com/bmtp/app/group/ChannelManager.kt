package com.bmtp.app.group

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages channels within a group.
 */
@Singleton
class ChannelManager @Inject constructor(
    private val groupRepository: GroupRepository,
    private val groupMembership: GroupMembership,
    private val config: GroupConfig,
    private val stats: GroupStatistics
) {
    fun createChannel(groupId: String, name: String, description: String, isPrivate: Boolean, requesterIdHex: String) {
        val group = groupRepository.getGroup(groupId) ?: throw GroupNotFoundException("Group not found")
        
        if (group.channels.size >= config.maxChannelsPerGroup) {
            throw SynchronizationException("Max channels limit reached")
        }
        
        val requesterRole = groupMembership.getRole(groupId, requesterIdHex) 
            ?: throw PermissionDeniedException("Requester not in group")
            
        // Assuming only Admins/Owners can create channels by default
        if (!requesterRole.canManage(MemberRole.ADMINISTRATOR)) {
            throw PermissionDeniedException("Insufficient permissions to create channel")
        }
        
        // Prevent duplicate names
        if (group.channels.values.any { it.name == name }) {
            throw GroupException("Channel with name '$name' already exists")
        }
        
        val newChannel = Channel(
            name = name, 
            description = description, 
            isPrivate = isPrivate
        )
        
        group.channels[newChannel.channelId] = newChannel
        group.versionTimestamp = System.currentTimeMillis()
        groupRepository.updateGroup(group)
        stats.recordChannelActivity()
    }

    fun deleteChannel(groupId: String, channelId: String, requesterIdHex: String) {
        val group = groupRepository.getGroup(groupId) ?: throw GroupNotFoundException("Group not found")
        val channel = group.channels[channelId] ?: throw ChannelNotFoundException("Channel not found")
        
        val requesterRole = groupMembership.getRole(groupId, requesterIdHex) 
            ?: throw PermissionDeniedException("Requester not in group")
            
        if (!channel.permissions.canManage(requesterRole)) {
            throw PermissionDeniedException("Insufficient permissions to delete channel")
        }
        
        if (channel.name == "general") {
            throw PermissionDeniedException("Cannot delete the default general channel")
        }
        
        group.channels.remove(channelId)
        group.versionTimestamp = System.currentTimeMillis()
        groupRepository.updateGroup(group)
        stats.recordChannelActivity()
    }

    fun updateChannelPermissions(groupId: String, channelId: String, permissions: ChannelPermissions, requesterIdHex: String) {
        val group = groupRepository.getGroup(groupId) ?: throw GroupNotFoundException("Group not found")
        val channel = group.channels[channelId] ?: throw ChannelNotFoundException("Channel not found")
        
        val requesterRole = groupMembership.getRole(groupId, requesterIdHex) 
            ?: throw PermissionDeniedException("Requester not in group")
            
        if (!channel.permissions.canManage(requesterRole)) {
            throw PermissionDeniedException("Insufficient permissions to update channel")
        }
        
        val updatedChannel = channel.copy(permissions = permissions)
        group.channels[channelId] = updatedChannel
        group.versionTimestamp = System.currentTimeMillis()
        groupRepository.updateGroup(group)
        stats.recordChannelActivity()
    }
}
