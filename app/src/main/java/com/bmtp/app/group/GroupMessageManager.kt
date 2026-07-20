package com.bmtp.app.group

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class GroupMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val groupId: String,
    val channelId: String,
    val senderIdHex: String,
    val payload: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Handles per-group ordering, encryption routing, and local persistence of messages.
 */
@Singleton
class GroupMessageManager @Inject constructor(
    private val groupRepository: GroupRepository,
    private val groupMembership: GroupMembership,
    private val groupEncryption: GroupEncryption,
    private val groupKeyManager: GroupKeyManager,
    private val config: GroupConfig,
    private val stats: GroupStatistics
) {
    // groupId -> (channelId -> List<GroupMessage>)
    private val messageCache = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableList<GroupMessage>>>()

    fun prepareMessageForBroadcast(
        groupId: String, 
        channelId: String, 
        senderIdHex: String, 
        rawPayload: ByteArray
    ): Pair<Int, ByteArray> { // Returns Epoch and EncryptedPayload
    
        val group = groupRepository.getGroup(groupId) ?: throw GroupNotFoundException("Group not found")
        val channel = group.channels[channelId] ?: throw ChannelNotFoundException("Channel not found")
        
        val senderRole = groupMembership.getRole(groupId, senderIdHex) 
            ?: throw PermissionDeniedException("Sender not in group")
            
        if (!channel.permissions.canWrite(senderRole)) {
            throw PermissionDeniedException("Insufficient permissions to write in channel")
        }
        
        // Forward Secrecy: Check if we need to rotate keys before sending
        groupKeyManager.checkAndRotateKey(groupId)
        
        // Encrypt the payload
        return groupEncryption.encrypt(groupId, rawPayload)
    }
    
    fun processIncomingMessage(
        groupId: String,
        channelId: String,
        senderIdHex: String,
        epoch: Int,
        encryptedPayload: ByteArray
    ): GroupMessage {
        val group = groupRepository.getGroup(groupId) ?: throw GroupNotFoundException("Group not found")
        val channel = group.channels[channelId] ?: throw ChannelNotFoundException("Channel not found")
        
        val senderRole = groupMembership.getRole(groupId, senderIdHex)
            ?: throw PermissionDeniedException("Sender not in group")
            
        if (!channel.permissions.canWrite(senderRole)) {
             throw PermissionDeniedException("Sender does not have write permissions")
        }
        
        // Decrypt the payload
        val decrypted = groupEncryption.decrypt(groupId, epoch, encryptedPayload)
        
        val message = GroupMessage(
            groupId = groupId,
            channelId = channelId,
            senderIdHex = senderIdHex,
            payload = decrypted
        )
        
        cacheMessage(message)
        stats.recordMessageSent() // Tracking total processed messages
        
        return message
    }
    
    private fun cacheMessage(message: GroupMessage) {
        val groupChannels = messageCache.computeIfAbsent(message.groupId) { ConcurrentHashMap() }
        val channelMessages = groupChannels.computeIfAbsent(message.channelId) { mutableListOf() }
        
        synchronized(channelMessages) {
            channelMessages.add(message)
            // Sort by logical timestamp for ordering
            channelMessages.sortBy { it.timestamp }
            
            // Enforce limit
            if (channelMessages.size > config.messageCacheLimit) {
                channelMessages.removeAt(0)
            }
        }
    }
    
    fun getMessages(groupId: String, channelId: String): List<GroupMessage> {
        val groupChannels = messageCache[groupId] ?: return emptyList()
        val channelMessages = groupChannels[channelId] ?: return emptyList()
        synchronized(channelMessages) {
            return channelMessages.toList()
        }
    }
}
