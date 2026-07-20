package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet

/**
 * Represents significant lifecycle events within the Mesh Engine.
 * Used for reactive logging and UI notifications.
 */
sealed interface MeshEvent {
    
    /** A new neighbor successfully connected to the mesh network. */
    data class NeighborJoined(val deviceId: String) : MeshEvent
    
    /** A neighbor disconnected or timed out. */
    data class NeighborLeft(val deviceId: String) : MeshEvent
    
    /** A packet was successfully forwarded to one or more neighbors. */
    data class PacketForwarded(val packetId: String, val neighborCount: Int) : MeshEvent
    
    /** A packet was dropped and will not be forwarded or processed. */
    data class PacketDropped(val packetId: String, val reason: String) : MeshEvent
    
    /** A packet was stored in the forward queue because no neighbors were available. */
    data class PacketQueued(val packetId: String) : MeshEvent
    
    /** A packet was successfully delivered to the local application layer. */
    data class PacketDelivered(val packet: Packet) : MeshEvent
    
    /** The store-and-forward queue reached its maximum capacity. */
    data object QueueFull : MeshEvent
}
