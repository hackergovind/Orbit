package com.bmtp.app.group

import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level facade for interacting with the Group Communication Layer.
 */
@Singleton
class GroupManager @Inject constructor(
    private val groupRepository: GroupRepository,
    private val groupMembership: GroupMembership,
    private val presenceManager: PresenceManager,
    private val channelManager: ChannelManager
) {
    /**
     * Creates a new decentralized group.
     */
    fun createGroup(name: String, description: String, isPublic: Boolean, ownerIdHex: String): String {
        val group = GroupState(
            name = name,
            description = description,
            isPublic = isPublic,
            ownerIdHex = ownerIdHex
        )
        
        groupRepository.addGroup(group)
        groupMembership.addMember(group.groupId, ownerIdHex, MemberRole.OWNER, MemberRole.OWNER)
        
        return group.groupId
    }
    
    fun getGroup(groupId: String): GroupState? {
        return groupRepository.getGroup(groupId)
    }

    fun deleteGroup(groupId: String, requesterIdHex: String) {
        val role = groupMembership.getRole(groupId, requesterIdHex) 
            ?: throw PermissionDeniedException("Requester not in group")
            
        if (role != MemberRole.OWNER) {
            throw PermissionDeniedException("Only the Owner can delete the group")
        }
        
        groupRepository.removeGroup(groupId)
    }

    // --- Membership Delegation ---
    
    fun joinPublicGroup(groupId: String, memberIdHex: String) {
        val group = groupRepository.getGroup(groupId) ?: throw GroupNotFoundException("Group not found")
        if (!group.isPublic) {
            throw PermissionDeniedException("Group is private, requires invitation")
        }
        
        // Members joining public groups start as GUEST
        groupMembership.addMember(groupId, memberIdHex, MemberRole.GUEST, MemberRole.OWNER)
    }

    fun changeMemberRole(groupId: String, targetIdHex: String, newRole: MemberRole, requesterIdHex: String) {
        val requesterRole = groupMembership.getRole(groupId, requesterIdHex) 
            ?: throw PermissionDeniedException("Requester not in group")
            
        groupMembership.changeRole(groupId, targetIdHex, newRole, requesterRole)
    }
    
    // --- Channel Delegation ---
    
    fun createChannel(groupId: String, name: String, description: String, isPrivate: Boolean, requesterIdHex: String) {
        channelManager.createChannel(groupId, name, description, isPrivate, requesterIdHex)
    }
    
    // --- Presence Delegation ---
    
    fun reportPresence(groupId: String, memberIdHex: String, status: PresenceStatus) {
        presenceManager.updatePresence(groupId, memberIdHex, status)
    }
}
