package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.ProtocolException
import com.bmtp.app.protocol.PacketValidator
import javax.inject.Inject

/**
 * Filter result indicating whether a packet should be processed or dropped.
 */
sealed interface FilterResult {
    data object Accept : FilterResult
    data class Reject(val reason: String) : FilterResult
}

/**
 * Responsible for dropping invalid, expired, or duplicate packets before they reach the routing logic.
 */
class PacketFilter @Inject constructor(
    private val packetValidator: PacketValidator,
    private val hopManager: HopManager
) {
    /**
     * Evaluates a packet against the mesh rules.
     * 
     * @param packet The packet to check.
     * @return [FilterResult.Accept] if valid, [FilterResult.Reject] with a reason if invalid.
     */
    fun evaluate(packet: Packet): FilterResult {
        try {
            // 1. Core protocol validation (includes duplicate check & TTL>0 check)
            packetValidator.validate(packet)
            
            // 2. Mesh-specific Hop limit validation
            hopManager.validate(packet.header.hopCount)
            
            return FilterResult.Accept
            
        } catch (e: ProtocolException) {
            return FilterResult.Reject(e.message ?: "Protocol exception")
        } catch (e: Exception) {
            return FilterResult.Reject("Unexpected error: ${e.message}")
        }
    }
}
