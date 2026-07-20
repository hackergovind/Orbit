package com.bmtp.app.transport

import com.bmtp.app.protocol.Packet
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory thread-safe queue for active transmissions.
 */
@Singleton
class PacketQueue @Inject constructor(
    private val config: TransportConfig,
    private val stats: TransportStatistics
) {
    private val queue = ConcurrentLinkedQueue<Packet>()
    private val currentSize = AtomicInteger(0)

    fun enqueue(packet: Packet): Boolean {
        if (currentSize.get() >= config.maxMemoryQueueSize) {
            throw QueueOverflowException("Memory queue overflow")
        }
        
        val added = queue.offer(packet)
        if (added) {
            currentSize.incrementAndGet()
            stats.recordQueued()
        }
        return added
    }

    fun dequeue(): Packet? {
        val packet = queue.poll()
        if (packet != null) {
            currentSize.decrementAndGet()
        }
        return packet
    }

    fun peek(): Packet? = queue.peek()

    fun isEmpty(): Boolean = queue.isEmpty()
    
    fun size(): Int = currentSize.get()
    
    fun clear() {
        queue.clear()
        currentSize.set(0)
    }
}
