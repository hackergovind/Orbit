package com.bmtp.app.group

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized logger for the Group Communication Layer.
 * Ensures strict compliance: "Never log decrypted message contents or group keys."
 */
@Singleton
class GroupLogger @Inject constructor() {
    private val subtag = "GroupMesh"

    fun logGroupCreated(groupId: String, name: String) {
        LogUtils.i(subtag, "Group Created: $groupId | Name: $name")
    }

    fun logGroupDeleted(groupId: String) {
        LogUtils.i(subtag, "Group Deleted: $groupId")
    }

    fun logMemberJoined(groupId: String, memberIdHex: String) {
        LogUtils.i(subtag, "Member Joined: $groupId | Member: $memberIdHex")
    }

    fun logMemberRemoved(groupId: String, memberIdHex: String) {
        LogUtils.w(subtag, "Member Removed: $groupId | Member: $memberIdHex")
    }

    fun logRoleChanged(groupId: String, memberIdHex: String, newRole: MemberRole) {
        LogUtils.i(subtag, "Role Changed: $groupId | Member: $memberIdHex | New Role: ${newRole.name}")
    }

    fun logInvitationAccepted(groupId: String, memberIdHex: String) {
        LogUtils.i(subtag, "Invitation Accepted: $groupId | Member: $memberIdHex")
    }

    fun logInvitationDeclined(groupId: String, memberIdHex: String) {
        LogUtils.i(subtag, "Invitation Declined: $groupId | Member: $memberIdHex")
    }

    fun logBroadcastSent(groupId: String, messageIdHex: String) {
        LogUtils.v(subtag, "Broadcast Sent: $groupId | MsgId: $messageIdHex")
    }

    fun logBroadcastReceived(groupId: String, messageIdHex: String) {
        LogUtils.v(subtag, "Broadcast Received: $groupId | MsgId: $messageIdHex")
    }
    
    fun logError(message: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, message, throwable)
    }
}
