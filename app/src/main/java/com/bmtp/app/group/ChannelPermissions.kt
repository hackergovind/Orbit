package com.bmtp.app.group

/**
 * Defines which roles have permissions within a specific channel.
 */
data class ChannelPermissions(
    /** Minimum role required to read messages in this channel. */
    val readRole: MemberRole = MemberRole.MEMBER,
    
    /** Minimum role required to send messages in this channel. */
    val writeRole: MemberRole = MemberRole.MEMBER,
    
    /** Minimum role required to invite others to this channel (if private). */
    val inviteRole: MemberRole = MemberRole.ADMINISTRATOR,
    
    /** Minimum role required to manage channel settings or delete it. */
    val manageRole: MemberRole = MemberRole.ADMINISTRATOR
) {
    fun canRead(userRole: MemberRole): Boolean = userRole.level <= readRole.level
    fun canWrite(userRole: MemberRole): Boolean = userRole.level <= writeRole.level
    fun canInvite(userRole: MemberRole): Boolean = userRole.level <= inviteRole.level
    fun canManage(userRole: MemberRole): Boolean = userRole.level <= manageRole.level
}
