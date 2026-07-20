package com.bmtp.app.security_hardening

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Protocol Versioning.
 * Ensures backward compatibility and graceful rejection of unsupported future versions.
 */
@Singleton
class VersionNegotiator @Inject constructor(
    private val config: GovernanceConfig,
    private val logger: AuditLogger
) {
    /**
     * Negotiates the highest mutually supported protocol version.
     * Throws VersionMismatchException if no compatible version is found.
     */
    @Throws(VersionMismatchException::class)
    fun negotiate(nodeId: String, peerSupportedVersions: Set<Int>): Int {
        val mutuallySupported = config.supportedProtocolVersions.intersect(peerSupportedVersions)
        
        if (mutuallySupported.isEmpty()) {
            logger.logVersionNegotiation(nodeId, peerSupportedVersions.maxOrNull() ?: 0, false)
            throw VersionMismatchException("No mutually supported protocol version. My versions: ${config.supportedProtocolVersions}, Peer versions: $peerSupportedVersions")
        }
        
        val highestVersion = mutuallySupported.maxOrNull()!!
        logger.logVersionNegotiation(nodeId, highestVersion, true)
        
        return highestVersion
    }
}
