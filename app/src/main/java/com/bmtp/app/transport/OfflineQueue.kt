package com.bmtp.app.transport

import com.bmtp.app.protocol.Packet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

data class QueuedPacket(
    val packet: Packet,
    val queuedAt: Long = System.currentTimeMillis()
)

/**
 * Queue for packets intended for unreachable destinations.
 * In a full app, this would be backed by disk persistence.
 */
@Singleton
class OfflineQueue @Inject constructor(
    private val config: TransportConfig,
    private val stats: TransportStatistics,
    private val logger: TransportLogger
) {
    // peerId -> Queue of Packets
    private val queues = ConcurrentHashMap<String, ConcurrentLinkedQueue<QueuedPacket>>()

    fun enqueue(destIdHex: String, packet: Packet) {
        val q = queues.computeIfAbsent(destIdHex) { ConcurrentLinkedQueue() }
        
        if (q.size >= config.maxOfflineQueueSize) {
            val oldest = q.poll()
            if (oldest != null) {
                stats.recordExpired()
                logger.logExpired("Evicted offline packet: ${oldest.packet.header.packetId.joinToString("") { "%02x".format(it) }}")
            }
        }
        
        q.offer(QueuedPacket(packet))
        stats.recordOfflineStored()
        val packetIdHex = packet.header.packetId.joinToString("") { "%02x".format(it) }
        logger.logOfflineStored(packetIdHex)
    }

    fun dequeueAllFor(destIdHex: String): List<Packet> {
        val q = queues[destIdHex] ?: return emptyList()
        val result = mutableListOf<Packet>()
        
        val currentTime = System.currentTimeMillis()
        
        var current = q.poll()
        while (current != null) {
            if (currentTime - current.queuedAt <= config.offlinePacketExpiryMs) {
                result.add(current.packet)
            } else {
                stats.recordExpired()
            }
            current = q.poll()
        }
        
        return result
    }
    
    fun removeExpired() {
        val currentTime = System.currentTimeMillis()
        queues.forEach { (_, q) ->
            val iterator = q.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (currentTime - item.queuedAt > config.offlinePacketExpiryMs) {
                    iterator.remove()
                    stats.recordExpired()
                }
            }
        }
    }
    
    // For persistence
    fun snapshot(): Map<String, List<QueuedPacket>> {
        val snap = mutableMapOf<String, List<QueuedPacket>>()
        queues.forEach { (k, v) -> snap[k] = v.toList() }
        return snap
    }
    
    fun restore(data: Map<String, List<QueuedPacket>>) {
        queues.clear()
        data.forEach { (k, v) ->
            val q = ConcurrentLinkedQueue<QueuedPacket>()
            q.addAll(v)
            queues[k] = q
        }
    }
}
