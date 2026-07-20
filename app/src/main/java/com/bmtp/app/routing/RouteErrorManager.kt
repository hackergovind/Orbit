package com.bmtp.app.routing

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a single unreachable destination in an RERR message.
 */
data class UnreachableDestination(
    val destId: ByteArray,
    val destSeqNum: UInt
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UnreachableDestination
        if (!destId.contentEquals(other.destId)) return false
        return destSeqNum == other.destSeqNum
    }
    
    override fun hashCode(): Int {
        return destId.contentHashCode() * 31 + destSeqNum.hashCode()
    }
}

/**
 * Format:
 * [DestCount (1)] + (DestCount * [DestId (16)] [DestSeqNum (4)])
 */
data class RerrPayload(
    val destinations: List<UnreachableDestination>
) {
    fun toByteArray(): ByteArray {
        val size = 1 + (destinations.size * 20)
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(destinations.size.toByte())
        
        for (dest in destinations) {
            buffer.put(dest.destId)
            buffer.putInt(dest.destSeqNum.toInt())
        }
        
        return buffer.array()
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): RerrPayload {
            require(bytes.isNotEmpty()) { "Invalid RERR payload size" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val destCount = buffer.get().toInt()
            
            require(bytes.size >= 1 + (destCount * 20)) { "Invalid RERR payload size for count $destCount" }
            
            val destinations = mutableListOf<UnreachableDestination>()
            for (i in 0 until destCount) {
                val destId = ByteArray(16)
                buffer.get(destId)
                val destSeqNum = buffer.getInt().toUInt()
                destinations.add(UnreachableDestination(destId, destSeqNum))
            }
            
            return RerrPayload(destinations)
        }
    }
}

/**
 * Handles creation and parsing of Route Error (RERR) packets.
 */
@Singleton
class RouteErrorManager @Inject constructor(
    private val packetFactory: PacketFactory
) {
    /**
     * Generates a new RERR packet to notify neighbors about broken links.
     */
    fun createRerr(myId: ByteArray, unreachableDests: List<UnreachableDestination>): Packet {
        val payload = RerrPayload(unreachableDests)
        
        return packetFactory.createRouteError(
            senderId = myId,
            payload = payload.toByteArray()
        )
    }

    fun parsePayload(payloadBytes: ByteArray): RerrPayload {
        return RerrPayload.fromByteArray(payloadBytes)
    }
}
