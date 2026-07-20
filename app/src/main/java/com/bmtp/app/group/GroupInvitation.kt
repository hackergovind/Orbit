package com.bmtp.app.group

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class InvitationToken(
    val tokenId: String,
    val groupId: String,
    val inviterIdHex: String,
    val targetRole: MemberRole,
    val expiresAt: Long,
    val signature: String // Cryptographic proof that inviter authorized this
)

@Singleton
class GroupInvitation @Inject constructor(
    private val groupMembership: GroupMembership,
    private val logger: GroupLogger,
    private val stats: GroupStatistics
) {
    // In-memory cache of pending invitations
    private val pendingInvitations = mutableMapOf<String, InvitationToken>()

    fun generateInvite(groupId: String, inviterIdHex: String, targetRole: MemberRole): InvitationToken {
        val inviterRole = groupMembership.getRole(groupId, inviterIdHex)
            ?: throw PermissionDeniedException("Inviter not in group")

        // Only Admins+ can invite, and they can't invite someone to a higher role than themselves
        if (!inviterRole.canManage(MemberRole.ADMINISTRATOR) || !inviterRole.canManage(targetRole)) {
            throw PermissionDeniedException("Insufficient role to generate this invitation")
        }

        val token = InvitationToken(
            tokenId = UUID.randomUUID().toString(),
            groupId = groupId,
            inviterIdHex = inviterIdHex,
            targetRole = targetRole,
            expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000L), // 24 hours
            signature = "mock_signature" // In real implementation, sign with inviter's private key
        )

        pendingInvitations[token.tokenId] = token
        return token
    }

    fun acceptInvite(tokenId: String, memberIdHex: String) {
        val token = pendingInvitations.remove(tokenId)
            ?: throw InvalidInvitationException("Invitation not found or revoked")
            
        if (System.currentTimeMillis() > token.expiresAt) {
            throw InvalidInvitationException("Invitation expired")
        }

        // Validate signature here using inviter's public key...

        val inviterRole = groupMembership.getRole(token.groupId, token.inviterIdHex)
        if (inviterRole == null || !inviterRole.canManage(token.targetRole)) {
            throw InvalidInvitationException("Inviter no longer has permission for this role")
        }

        groupMembership.addMember(token.groupId, memberIdHex, token.targetRole, inviterRole)
        logger.logInvitationAccepted(token.groupId, memberIdHex)
    }

    fun declineInvite(tokenId: String, memberIdHex: String) {
        val token = pendingInvitations.remove(tokenId)
        if (token != null) {
            logger.logInvitationDeclined(token.groupId, memberIdHex)
        }
    }
}
