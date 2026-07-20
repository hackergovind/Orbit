package com.bmtp.app.voice

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts and decrypts real-time voice payloads.
 * We mock AES-GCM or ChaCha20-Poly1305 authenticated encryption here.
 */
@Singleton
class VoiceEncryption @Inject constructor(
    private val logger: VoiceLogger
) {
    /**
     * Encrypts a raw Opus frame using the Session Key.
     */
    fun encrypt(sessionKey: ByteArray, rawFrame: ByteArray): ByteArray {
        // In reality, this would use a Cipher (e.g. AES/GCM/NoPadding)
        // For simulation, we XOR with the first byte of the key
        val mockEncrypted = rawFrame.map { (it.toInt() xor sessionKey[0].toInt()).toByte() }.toByteArray()
        return mockEncrypted
    }

    /**
     * Decrypts and authenticates a received voice frame.
     * Throws VoiceEncryptionException if the authentication tag fails (tampering).
     */
    fun decrypt(sessionKey: ByteArray, encryptedFrame: ByteArray): ByteArray {
        try {
            // Mock Decryption logic
            val mockDecrypted = encryptedFrame.map { (it.toInt() xor sessionKey[0].toInt()).toByte() }.toByteArray()
            
            // Mock authentication check: If someone maliciously flipped bits without updating the auth tag
            // we would throw here.
            if (encryptedFrame.isNotEmpty() && encryptedFrame[0] == 0xDE.toByte()) {
                 throw VoiceEncryptionException("Authentication tag failed. Packet tampered with.")
            }
            
            return mockDecrypted
        } catch (e: Exception) {
            logger.logError("Voice decryption failed", e)
            throw VoiceEncryptionException("Failed to decrypt voice packet", e)
        }
    }
}
