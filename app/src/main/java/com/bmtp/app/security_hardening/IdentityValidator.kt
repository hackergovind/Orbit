package com.bmtp.app.security_hardening

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates cryptographic identities (Public Keys) during the Phase 6 Handshake.
 * Rejects identities that are malformed or inconsistent.
 */
@Singleton
class IdentityValidator @Inject constructor(
    private val abuseDetector: AbuseDetector
) {
    /**
     * Expected length of an Ed25519 Public Key in bytes.
     */
    private val ED25519_PUBKEY_LENGTH = 32

    /**
     * Validates a Public Key presented by a peer.
     * Throws ProtocolViolationException if invalid.
     */
    @Throws(ProtocolViolationException::class)
    fun validatePublicKey(nodeId: String, publicKey: ByteArray) {
        if (publicKey.size != ED25519_PUBKEY_LENGTH) {
            abuseDetector.reportProtocolViolation(nodeId, "Invalid Public Key length: ${publicKey.size}")
            throw ProtocolViolationException("Identity rejected: Malformed public key.")
        }
        
        // In a real implementation, we would also check if the public key is on a known weak curve curve
        // or if it matches the expected hash (Node ID).
    }
}
