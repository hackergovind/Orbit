package com.bmtp.app.group

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts and decrypts group payloads (messages, broadcasts, sync events)
 * using the symmetric group key managed by GroupKeyManager.
 */
@Singleton
class GroupEncryption @Inject constructor(
    private val keyManager: GroupKeyManager
) {
    /**
     * Encrypts the payload for the current group epoch.
     * Returns a pair of (Epoch, EncryptedPayload).
     */
    fun encrypt(groupId: String, payload: ByteArray): Pair<Int, ByteArray> {
        val currentEpoch = keyManager.getCurrentEpoch(groupId)
        val key = keyManager.getEncryptionKey(groupId, currentEpoch)
        
        // Mock AES-GCM encryption
        val encrypted = payload.map { (it.toInt() xor key[0].toInt()).toByte() }.toByteArray()
        return Pair(currentEpoch, encrypted)
    }

    /**
     * Decrypts a payload that was encrypted with a specific epoch key.
     */
    fun decrypt(groupId: String, epoch: Int, encryptedPayload: ByteArray): ByteArray {
        try {
            val key = keyManager.getEncryptionKey(groupId, epoch)
            // Mock AES-GCM decryption
            val decrypted = encryptedPayload.map { (it.toInt() xor key[0].toInt()).toByte() }.toByteArray()
            return decrypted
        } catch (e: Exception) {
            throw GroupEncryptionException("Failed to decrypt group payload for epoch $epoch", e)
        }
    }
}
