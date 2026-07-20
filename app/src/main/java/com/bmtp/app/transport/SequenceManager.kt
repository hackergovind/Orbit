package com.bmtp.app.transport

import com.bmtp.app.protocol.Packet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles incoming packets to ensure they are delivered to the application exactly once and in order.
 */
@Singleton
class SequenceManager @Inject constructor(
    private val sessionState: SessionState,
    private val config: TransportConfig,
    private val logger: TransportLogger
) {
    // Buffers packets that arrive early. Key is senderIdHex.
    private val outOfOrderBuffers = ConcurrentHashMap<String, PriorityBlockingQueue<SequencedPacket>>()

    private data class SequencedPacket(
        val sequenceNumber: UInt,
        val packet: Packet,
        val receivedAt: Long = System.currentTimeMillis()
    ) : Comparable<SequencedPacket> {
        override fun compareTo(other: SequencedPacket): Int {
            return this.sequenceNumber.compareTo(other.sequenceNumber)
        }
    }

    /**
     * Processes an incoming packet.
     * Returns a list of packets that are now ready to be delivered to the application in order.
     */
    fun processIncomingPacket(senderIdHex: String, sequenceNumber: UInt, packet: Packet): List<Packet> {
        val expectedSeq = sessionState.expectedIncomingSequence(senderIdHex)
        val readyToDeliver = mutableListOf<Packet>()

        if (sequenceNumber < expectedSeq) {
            // This shouldn't happen if DuplicateDetector runs first, but handles wraparound edge cases
            logger.logError("Received old sequence $sequenceNumber from $senderIdHex. Expected $expectedSeq", null)
            return emptyList()
        }

        if (sequenceNumber == expectedSeq) {
            // In order! Deliver this one.
            readyToDeliver.add(packet)
            var nextExpected = expectedSeq + 1u
            
            // Check if we can now deliver buffered packets
            val buffer = outOfOrderBuffers[senderIdHex]
            if (buffer != null) {
                while (buffer.isNotEmpty() && buffer.peek()?.sequenceNumber == nextExpected) {
                    val bufferedPacket = buffer.poll()
                    if (bufferedPacket != null) {
                        readyToDeliver.add(bufferedPacket.packet)
                        nextExpected++
                    }
                }
            }
            sessionState.updateIncomingSequence(senderIdHex, nextExpected)
        } else {
            // Out of order (arrived early)
            val buffer = outOfOrderBuffers.computeIfAbsent(senderIdHex) { PriorityBlockingQueue() }
            buffer.offer(SequencedPacket(sequenceNumber, packet))
            logger.logError("Packet arrived early from $senderIdHex. Seq: $sequenceNumber, Expected: $expectedSeq", null)
        }

        return readyToDeliver
    }
    
    fun evictStaleBufferedPackets() {
        val currentTime = System.currentTimeMillis()
        outOfOrderBuffers.forEach { (peerId, buffer) ->
            val iterator = buffer.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (currentTime - item.receivedAt > config.reorderWindowMs) {
                    iterator.remove()
                    logger.logExpired("Out-of-order packet expired: seq ${item.sequenceNumber} from $peerId")
                    
                    // If a packet expires, we must advance the expected sequence number so we don't block forever
                    val expected = sessionState.expectedIncomingSequence(peerId)
                    if (item.sequenceNumber == expected) {
                         sessionState.updateIncomingSequence(peerId, expected + 1u)
                    }
                }
            }
        }
    }
}
