package com.bmtp.app.routing

/**
 * Represents the current operational state of the Routing Engine.
 */
sealed interface RoutingState {
    /** Routing engine initialized but inactive. */
    data object Idle : RoutingState
    
    /** Processing normal traffic and maintaining routes. */
    data object Ready : RoutingState
    
    /** Actively searching for one or more routes. */
    data object Discovering : RoutingState
    
    /** Attempting to repair a broken route locally. */
    data object Repairing : RoutingState
    
    /** Recovering from a major failure. */
    data object Recovering : RoutingState
    
    /** Fatal error state. */
    data class Error(val message: String) : RoutingState
}
