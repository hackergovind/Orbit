package com.bmtp.app.group

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks who is in which group and their roles.
 */
@Singleton
class GroupMembership @Inject constructor(
    private val config: GroupConfig,
    private val logger: GroupLogger,
    private val stats: GroupStatistics
) {
    // Map of GroupId -> (Map of MemberHexId -> MemberRole)
    private val membership = ConcurrentHashMap<String, ConcurrentHashMap<String, MemberRole>>()

    fun addMember(groupId: String, memberIdHex: String, role: MemberRole, inviterRole: MemberRole) {
        // Enforce permissions: Only Admins/Owners can add directly without invite
        if (role != MemberRole.OWNER && !inviterRole.canManage(MemberRole.ADMINISTRATOR)) {
            throw PermissionDeniedException("Insufficient role to add members directly")
        }
        
        val groupMembers = membership.computeIfAbsent(groupId) { ConcurrentHashMap() }
        
        if (groupMembers.size >= config.maxMembersPerGroup) {
            throw SynchronizationException("Max members limit reached for group $groupId")
        }
        
        if (groupMembers.containsKey(memberIdHex)) {
            throw AlreadyMemberException("Member $memberIdHex is already in group $groupId")
        }
        
        groupMembers[memberIdHex] = role
        stats.recordMembershipChange()
        logger.logMemberJoined(groupId, memberIdHex)
        updateActiveMembersTotal()
    }

    fun removeMember(groupId: String, memberIdHex: String, requesterIdHex: String, requesterRole: MemberRole) {
        val groupMembers = membership[groupId] ?: throw GroupNotFoundException("Group $groupId not found")
        val targetRole = groupMembers[memberIdHex] ?: throw GroupException("Member not found in group")
        
        // Self-leave or Admin removal
        if (memberIdHex != requesterIdHex) {
            if (!requesterRole.canManage(targetRole)) {
                throw PermissionDeniedException("Cannot remove member with higher or equal role")
            }
        }
        
        groupMembers.remove(memberIdHex)
        stats.recordMembershipChange()
        logger.logMemberRemoved(groupId, memberIdHex)
        updateActiveMembersTotal()
    }

    fun changeRole(groupId: String, targetIdHex: String, newRole: MemberRole, requesterRole: MemberRole) {
        val groupMembers = membership[groupId] ?: throw GroupNotFoundException("Group $groupId not found")
        val currentTargetRole = groupMembers[targetIdHex] ?: throw GroupException("Member not found in group")
        
        if (!requesterRole.canManage(currentTargetRole) || !requesterRole.canManage(newRole)) {
            throw PermissionDeniedException("Cannot modify role equal or higher than your own")
        }
        
        if (newRole == MemberRole.OWNER && currentTargetRole != MemberRole.OWNER) {
             throw RoleConflictException("Use ownership transfer to change owner")
        }
        
        groupMembers[targetIdHex] = newRole
        stats.recordMembershipChange()
        logger.logRoleChanged(groupId, targetIdHex, newRole)
    }

    fun getRole(groupId: String, memberIdHex: String): MemberRole? {
        return membership[groupId]?.get(memberIdHex)
    }

    fun getMembers(groupId: String): Map<String, MemberRole> {
        return membership[groupId]?.toMap() ?: emptyMap()
    }
    
    private fun updateActiveMembersTotal() {
        val total = membership.values.sumOf { it.size }
        stats.updateActiveMembers(total)
    }
}
