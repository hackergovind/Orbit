package com.bmtp.app.group

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Group Encryption Keys.
 * Handles "Lazy Rekeying" to satisfy Forward Secrecy without massive immediate bandwidth spikes.
 */
@Singleton
class GroupKeyManager @Inject constructor(
    private val groupRepository: GroupRepository,
    private val logger: GroupLogger
) {
    // groupId -> (epoch -> SymmetricKey)
    private val groupKeys = ConcurrentHashMap<String, ConcurrentHashMap<Int, ByteArray>>()
    
    // Tracks if a group needs rekeying on the next message send
    private val pendingRekey = ConcurrentHashMap<String, Boolean>()

    fun getEncryptionKey(groupId: String, epoch: Int): ByteArray {
        val keys = groupKeys[groupId] ?: throw GroupEncryptionException("No keys found for group $groupId")
        return keys[epoch] ?: throw GroupEncryptionException("Key not found for epoch $epoch")
    }

    fun getCurrentEpoch(groupId: String): Int {
        val group = groupRepository.getGroup(groupId) ?: throw GroupNotFoundException("Group not found")
        return group.keyEpoch
    }

    /**
     * Called when a group is created.
     */
    fun initializeGroupKey(groupId: String) {
        val keys = ConcurrentHashMap<Int, ByteArray>()
        keys[1] = generateSymmetricKey() // Epoch 1
        groupKeys[groupId] = keys
        pendingRekey[groupId] = false
    }

    /**
     * Marks a group as needing a key rotation (e.g., after a member is removed).
     */
    fun markForRekey(groupId: String) {
        pendingRekey[groupId] = true
    }

    /**
     * Checks if a rekey is needed, and if so, rotates the key and returns the new epoch and key.
     * This is called lazily right before a message is sent.
     */
    fun checkAndRotateKey(groupId: String): Pair<Int, ByteArray>? {
        if (pendingRekey[groupId] == true) {
            val group = groupRepository.getGroup(groupId) ?: return null
            val newEpoch = group.keyEpoch + 1
            
            val keys = groupKeys.computeIfAbsent(groupId) { ConcurrentHashMap() }
            val newKey = generateSymmetricKey()
            keys[newEpoch] = newKey
            
            group.keyEpoch = newEpoch
            groupRepository.updateGroup(group)
            
            pendingRekey[groupId] = false
            logger.logError("Rotated group key for $groupId to epoch $newEpoch", null)
            
            return Pair(newEpoch, newKey)
        }
        return null
    }

    private fun generateSymmetricKey(): ByteArray {
        // In reality, use SecureRandom to generate AES-256 key
        return ByteArray(32) { (Math.random() * 256).toInt().toByte() }
    }
}
