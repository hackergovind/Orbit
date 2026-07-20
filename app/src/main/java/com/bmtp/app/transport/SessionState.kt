package com.bmtp.app.transport

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sequence numbers for encrypted transport sessions.
 * Each peer has its own incoming and outgoing sequence number.
 */
@Singleton
class SessionState @Inject constructor() {
    
    // peerId (hex) -> Sequence Number
    private val incomingSequences = ConcurrentHashMap<String, AtomicInteger>()
    private val outgoingSequences = ConcurrentHashMap<String, AtomicInteger>()

    /**
     * Gets and increments the outgoing sequence number for a given peer.
     */
    fun nextOutgoingSequence(peerIdHex: String): UInt {
        val seq = outgoingSequences.computeIfAbsent(peerIdHex) { AtomicInteger(0) }
        return seq.incrementAndGet().toUInt()
    }

    /**
     * Retrieves the expected next incoming sequence number for a given peer.
     */
    fun expectedIncomingSequence(peerIdHex: String): UInt {
        val seq = incomingSequences.computeIfAbsent(peerIdHex) { AtomicInteger(1) }
        return seq.get().toUInt()
    }

    /**
     * Updates the expected incoming sequence number for a given peer.
     */
    fun updateIncomingSequence(peerIdHex: String, nextExpected: UInt) {
        val seq = incomingSequences.computeIfAbsent(peerIdHex) { AtomicInteger(1) }
        seq.set(nextExpected.toInt())
    }

    /**
     * Restores state from persistence.
     */
    fun restore(incoming: Map<String, Int>, outgoing: Map<String, Int>) {
        incoming.forEach { (k, v) -> incomingSequences[k] = AtomicInteger(v) }
        outgoing.forEach { (k, v) -> outgoingSequences[k] = AtomicInteger(v) }
    }

    fun snapshotIncoming(): Map<String, Int> = incomingSequences.mapValues { it.value.get() }
    fun snapshotOutgoing(): Map<String, Int> = outgoingSequences.mapValues { it.value.get() }
}
