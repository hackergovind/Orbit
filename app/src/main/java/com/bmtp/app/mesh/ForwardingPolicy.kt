package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Defines the strategy for selecting which neighbors should receive a forwarded packet.
 */
interface ForwardingPolicy {
    /**
     * Determines the subset of connected neighbors that should receive the packet.
     *
     * @param packet The packet to forward.
     * @param connectedNeighbors The set of all currently connected device IDs.
     * @param sourceDeviceId The device ID from which the packet was received, or null if self-originated.
     * @return The set of device IDs that should receive the packet.
     */
    fun selectTargets(packet: Packet, connectedNeighbors: Set<String>, sourceDeviceId: String?): Set<String>
}

/**
 * Implementation of controlled flooding.
 * Forwards the packet to all connected neighbors EXCEPT the one it just arrived from.
 */
@Singleton
class ControlledFloodingPolicy @Inject constructor() : ForwardingPolicy {

    override fun selectTargets(packet: Packet, connectedNeighbors: Set<String>, sourceDeviceId: String?): Set<String> {
        // Filter out the neighbor that sent us the packet to prevent immediate echo loops.
        return if (sourceDeviceId != null) {
            connectedNeighbors - sourceDeviceId
        } else {
            connectedNeighbors
        }
    }
}
