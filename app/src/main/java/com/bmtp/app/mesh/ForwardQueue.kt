package com.bmtp.app.mesh

import com.bmtp.app.protocol.Packet
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for a packet in the queue, retaining its insertion time to handle expiration.
 */
data class QueuedPacket(
    val packet: Packet,
    val enqueuedAt: Long = System.currentTimeMillis()
)

/**
 * Store-and-forward queue for packets that need to be sent but currently have no neighbors.
 * Uses a FIFO approach and drops old packets when the limit is reached.
 */
@Singleton
class ForwardQueue @Inject constructor(
    private val config: MeshConfig,
    private val logger: MeshLogger,
    private val stats: MeshStatistics
) {
    private val queue = ConcurrentLinkedQueue<QueuedPacket>()

    /**
     * Adds a packet to the queue. If the queue is full, the oldest packet is dropped.
     *
     * @param packet The packet to enqueue.
     */
    fun enqueue(packet: Packet) {
        if (queue.size >= config.forwardQueueSize) {
            val dropped = queue.poll()
            if (dropped != null) {
                logger.logDropped(dropped.packet.header.packetIdAsString(), "ForwardQueue full")
                stats.incrementDropped()
            }
        }
        queue.add(QueuedPacket(packet))
        logger.logQueued(packet.header.packetIdAsString())
        stats.updateQueueSize(queue.size)
    }

    /**
     * Retrieves all valid (non-expired) packets from the queue and clears it.
     *
     * @return A list of valid packets to retry forwarding.
     */
    fun dequeueAllValid(): List<Packet> {
        val now = System.currentTimeMillis()
        val validPackets = mutableListOf<Packet>()
        
        while (queue.isNotEmpty()) {
            val queued = queue.poll()
            if (queued != null) {
                if (now - queued.enqueuedAt <= config.packetExpirationMs) {
                    validPackets.add(queued.packet)
                } else {
                    logger.logDropped(queued.packet.header.packetIdAsString(), "Expired in ForwardQueue")
                    stats.incrementDropped()
                }
            }
        }
        
        stats.updateQueueSize(0)
        return validPackets
    }

    /**
     * Checks if the queue is empty.
     */
    fun isEmpty(): Boolean = queue.isEmpty()
}
