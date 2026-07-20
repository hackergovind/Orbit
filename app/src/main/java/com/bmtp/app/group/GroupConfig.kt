package com.bmtp.app.group

/**
 * Configuration parameters for the BMTP Distributed Group Communication Layer.
 */
data class GroupConfig(
    /** Maximum number of members allowed in a single group to prevent excessive sync overhead. */
    val maxMembersPerGroup: Int = 10_000,
    
    /** Maximum number of groups a single node can join. */
    val maxJoinedGroups: Int = 1_000,
    
    /** Maximum number of channels allowed within a single group. */
    val maxChannelsPerGroup: Int = 100,
    
    /** Interval in milliseconds for sending presence heartbeats (e.g., 5 minutes). */
    val presenceHeartbeatIntervalMs: Long = 5 * 60 * 1000L,
    
    /** Time in milliseconds before a node is considered offline if no heartbeat is received (e.g., 15 minutes). */
    val presenceOfflineThresholdMs: Long = 15 * 60 * 1000L,
    
    /** Interval in milliseconds for background synchronization of group state (e.g., 1 hour). */
    val syncIntervalMs: Long = 60 * 60 * 1000L,
    
    /** Maximum number of messages to retain in memory per group before flushing to persistence. */
    val messageCacheLimit: Int = 500,
    
    /** Number of hops (TTL) for a group broadcast message. */
    val defaultBroadcastTtl: Int = 7
)
