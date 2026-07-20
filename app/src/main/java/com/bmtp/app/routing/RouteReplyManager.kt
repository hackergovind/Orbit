package com.bmtp.app.routing

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Format:
 * [DestId (16)] [DestSeqNum (4)] [OrigId (16)] [HopCount (1)] [Lifetime (4)]
 */
data class RrepPayload(
    val destId: ByteArray,
    val destSeqNum: UInt,
    val origId: ByteArray,
    val hopCount: UByte,
    val lifetimeMs: UInt
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(41).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(destId)
        buffer.putInt(destSeqNum.toInt())
        buffer.put(origId)
        buffer.put(hopCount.toByte())
        buffer.putInt(lifetimeMs.toInt())
        return buffer.array()
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): RrepPayload {
            require(bytes.size >= 41) { "Invalid RREP payload size" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val destId = ByteArray(16)
            buffer.get(destId)
            val destSeqNum = buffer.getInt().toUInt()
            val origId = ByteArray(16)
            buffer.get(origId)
            val hopCount = buffer.get().toUByte()
            val lifetimeMs = buffer.getInt().toUInt()
            
            return RrepPayload(destId, destSeqNum, origId, hopCount, lifetimeMs)
        }
    }
}

/**
 * Handles creation and parsing of Route Reply (RREP) packets.
 */
@Singleton
class RouteReplyManager @Inject constructor(
    private val packetFactory: PacketFactory,
    private val config: RoutingConfig,
    private val seqNumManager: SequenceNumberManager
) {
    /**
     * Generates a new RREP packet. This is called by the destination node
     * when it receives an RREQ intended for itself.
     */
    fun createRrep(myId: ByteArray, originatorId: ByteArray): Packet {
        val payload = RrepPayload(
            destId = myId,
            destSeqNum = seqNumManager.getCurrent(),
            origId = originatorId,
            hopCount = 0u,
            lifetimeMs = config.routeLifetimeMs.toUInt()
        )
        
        return packetFactory.createRouteReply(
            senderId = myId,
            receiverId = ByteArray(16), // RREP is unicast, MeshEngine determines next hop based on originatorId route
            payload = payload.toByteArray()
        )
    }

    /**
     * Creates a forwarded version of an RREP packet.
     * Hop count is incremented.
     */
    fun createForwardedRrep(myId: ByteArray, parsedPayload: RrepPayload): Packet {
        val forwardedPayload = parsedPayload.copy(
            hopCount = (parsedPayload.hopCount + 1u).toUByte()
        )
        return packetFactory.createRouteReply(
            senderId = myId,
            receiverId = ByteArray(16), // Overwritten during unicast dispatch
            payload = forwardedPayload.toByteArray()
        )
    }

    fun parsePayload(payloadBytes: ByteArray): RrepPayload {
        return RrepPayload.fromByteArray(payloadBytes)
    }
}
