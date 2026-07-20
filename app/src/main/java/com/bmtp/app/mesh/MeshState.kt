package com.bmtp.app.mesh

/**
 * Represents the current operational state of the Mesh Engine.
 */
sealed interface MeshState {
    /** The engine is initialized but not active. */
    data object Idle : MeshState
    
    /** The engine is discovering neighbors. */
    data object Discovering : MeshState
    
    /** The engine is connected to the mesh network. */
    data object Connected : MeshState
    
    /** The engine is actively relaying packets. */
    data object Relaying : MeshState
    
    /** The engine is waiting for neighbors to connect (store-and-forward active). */
    data object Waiting : MeshState
    
    /** The forward queue is at maximum capacity. */
    data object QueueFull : MeshState
    
    /** The engine is attempting to recover from a failure state. */
    data object Recovering : MeshState
    
    /** The engine encountered a fatal error. */
    data class Error(val message: String) : MeshState
}
