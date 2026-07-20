package com.bmtp.app.group

import java.util.UUID

/**
 * Defines a Sub-space or Channel within a Group.
 */
data class Channel(
    val channelId: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val isPrivate: Boolean = false,
    val permissions: ChannelPermissions = ChannelPermissions()
)

/**
 * Core metadata model for a BMTP Group.
 */
data class GroupState(
    val groupId: String = UUID.randomUUID().toString(),
    var name: String,
    val ownerIdHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Logical clock for resolving concurrent updates (LWW Eventual Consistency) */
    var versionTimestamp: Long = System.currentTimeMillis(),
    
    /** Identifies the current symmetric encryption key epoch */
    var keyEpoch: Int = 1,
    
    var description: String = "",
    var isPublic: Boolean = false,
    
    val channels: MutableMap<String, Channel> = mutableMapOf()
) {
    init {
        // Ensure every group has at least a default "general" channel
        if (channels.isEmpty()) {
            val general = Channel(name = "general", description = "General Discussion")
            channels[general.channelId] = general
        }
    }
}
