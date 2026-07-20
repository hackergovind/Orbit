package com.bmtp.app.routing

/**
 * Status of a route in the routing table.
 */
enum class RouteStatus {
    /** Route is valid and actively available for use. */
    ACTIVE,
    
    /** Route is no longer valid and shouldn't be used, but kept to retain Sequence Number. */
    INVALID
}

/**
 * A single entry in the AODV routing table.
 */
data class RouteEntry(
    /** The ultimate destination node ID (hex string). */
    val destinationId: String,
    
    /** The neighbor node ID to send packets to in order to reach the destination. */
    val nextHopId: String,
    
    /** Number of hops to reach the destination. */
    val hopCount: UByte,
    
    /** The latest sequence number known for the destination. */
    val sequenceNumber: UInt,
    
    /** Expiration timestamp in milliseconds. */
    val expiryTimestamp: Long,
    
    /** Current status of the route. */
    val status: RouteStatus = RouteStatus.ACTIVE,
    
    /** Optional metrics for route selection logic. */
    val score: Int = 0,
    
    /** Timestamp when this route was created. */
    val createdTimestamp: Long = System.currentTimeMillis()
)
