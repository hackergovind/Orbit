package com.bmtp.app.routing

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the monotonic sequence number for this node.
 * Sequence numbers are critical in AODV for preventing routing loops
 * and ensuring freshness of route information.
 */
@Singleton
class SequenceNumberManager @Inject constructor() {
    
    // Start at 1. UInt is used in protocol, but AtomicInteger handles safe incrementation.
    // In a real app, this might be persisted to disk to survive reboots.
    private val currentSequenceNumber = AtomicInteger(1)

    /**
     * Gets the current sequence number without incrementing it.
     */
    fun getCurrent(): UInt {
        return currentSequenceNumber.get().toUInt()
    }

    /**
     * Increments and returns the new sequence number.
     * Must be called whenever this node's routing neighborhood changes
     * (e.g., creating a new RREQ or detecting a link break).
     */
    fun incrementAndGet(): UInt {
        // Handle overflow safely for UInt mapping
        return currentSequenceNumber.incrementAndGet().toUInt()
    }
}
