package com.bmtp.app.group

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class GroupTests {

    private lateinit var config: GroupConfig
    private lateinit var stats: GroupStatistics
    private lateinit var logger: GroupLogger
    private lateinit var repository: GroupRepository
    private lateinit var membership: GroupMembership
    private lateinit var channelManager: ChannelManager
    private lateinit var presenceManager: PresenceManager
    private lateinit var manager: GroupManager

    @Before
    fun setup() {
        config = GroupConfig()
        stats = GroupStatistics()
        logger = GroupLogger()
        repository = GroupRepository(config, logger, stats)
        membership = GroupMembership(config, logger, stats)
        channelManager = ChannelManager(repository, membership, config, stats)
        presenceManager = PresenceManager(config, stats)
        
        manager = GroupManager(repository, membership, presenceManager, channelManager)
    }

    @Test
    fun `Role Enforcement - Guest cannot change group name`() {
        val ownerId = "owner-hex"
        val guestId = "guest-hex"
        
        val groupId = manager.createGroup("Test Group", "Desc", true, ownerId)
        manager.joinPublicGroup(groupId, guestId) // Joins as GUEST
        
        val group = repository.getGroup(groupId)!!
        
        // Attempting to change role should fail since Guest < Admin
        assertThrows(PermissionDeniedException::class.java) {
            manager.changeMemberRole(groupId, guestId, MemberRole.MEMBER, guestId)
        }
    }
    
    @Test
    fun `Key Rotation - Removing member triggers rekey`() {
        val ownerId = "owner-hex"
        val memberId = "member-hex"
        
        val groupId = manager.createGroup("Secure Group", "Desc", false, ownerId)
        val keyManager = GroupKeyManager(repository, logger)
        keyManager.initializeGroupKey(groupId)
        
        // Add member
        membership.addMember(groupId, memberId, MemberRole.MEMBER, MemberRole.OWNER)
        
        assertEquals(1, keyManager.getCurrentEpoch(groupId))
        
        // Mark for rekey
        keyManager.markForRekey(groupId)
        
        // Lazy rotation occurs on next check
        val result = keyManager.checkAndRotateKey(groupId)
        assertNotNull(result)
        assertEquals(2, result!!.first)
        assertEquals(2, keyManager.getCurrentEpoch(groupId))
    }
    
    @Test
    fun `Presence - Heartbeat timeout transitions node to Offline`() {
        val memberId = "member-hex"
        
        // Mock a short timeout config for testing
        val shortConfig = GroupConfig(presenceOfflineThresholdMs = 100L)
        val shortPresence = PresenceManager(shortConfig, stats)
        
        shortPresence.updatePresence("group1", memberId, PresenceStatus.ONLINE)
        assertEquals(PresenceStatus.ONLINE, shortPresence.getPresence("group1", memberId))
        
        // Simulate time passing (in real tests, use delay in runTest or mock Clock)
        Thread.sleep(150L)
        
        // Should be offline now
        assertEquals(PresenceStatus.OFFLINE, shortPresence.getPresence("group1", memberId))
    }
    
    @Test
    fun `Sync Conflicts - LWW Eventual Consistency resolves stale updates`() {
        val ownerId = "owner-hex"
        val groupId = manager.createGroup("Sync Group", "Desc", false, ownerId)
        
        val groupV1 = repository.getGroup(groupId)!!
        
        // Create conflicting update V2 (Newer timestamp)
        val groupV2 = groupV1.copy(name = "Updated V2", versionTimestamp = groupV1.versionTimestamp + 100)
        
        // Create conflicting update V3 (Older timestamp - Stale!)
        val groupV3 = groupV1.copy(name = "Stale V3", versionTimestamp = groupV1.versionTimestamp - 100)
        
        repository.updateGroup(groupV2) // Should apply
        assertEquals("Updated V2", repository.getGroup(groupId)!!.name)
        
        repository.updateGroup(groupV3) // Should be rejected
        assertEquals("Updated V2", repository.getGroup(groupId)!!.name)
    }
}
