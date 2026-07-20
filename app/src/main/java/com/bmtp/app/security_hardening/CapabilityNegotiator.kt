package com.bmtp.app.security_hardening

import javax.inject.Inject
import javax.inject.Singleton

enum class BmtpCapability {
    VOICE_V1,
    FILE_TRANSFER_V1,
    GROUPS_V1,
    ENCRYPTION_CHACHA20, // Example future capability
    COMPRESSION_LZ4      // Example future capability
}

/**
 * Negotiates feature support between nodes.
 * Ensures that nodes only attempt advanced operations (like Voice) if the peer supports it.
 */
@Singleton
class CapabilityNegotiator @Inject constructor() {
    
    private val myCapabilities = setOf(
        BmtpCapability.VOICE_V1,
        BmtpCapability.FILE_TRANSFER_V1,
        BmtpCapability.GROUPS_V1
    )

    /**
     * Returns the capabilities supported by this node to be advertised during handshake.
     */
    fun getMyCapabilities(): Set<BmtpCapability> = myCapabilities

    /**
     * Checks if a peer supports a specific required capability.
     * Throws CapabilityMismatchException if they do not.
     */
    @Throws(CapabilityMismatchException::class)
    fun requireCapability(nodeId: String, peerCapabilities: Set<BmtpCapability>, required: BmtpCapability) {
        if (!peerCapabilities.contains(required)) {
            throw CapabilityMismatchException("Node $nodeId does not support required capability: $required")
        }
    }
    
    /**
     * Finds the intersection of capabilities to determine mutually supported features.
     */
    fun getMutuallySupported(peerCapabilities: Set<BmtpCapability>): Set<BmtpCapability> {
        return myCapabilities.intersect(peerCapabilities)
    }
}
