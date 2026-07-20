package com.bmtp.app.routing

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketFactory
import com.bmtp.app.protocol.ProtocolConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses and generates RREQ payloads.
 * Format:
 * [DestId (16)] [DestSeqNum (4)] [OrigId (16)] [OrigSeqNum (4)] [HopCount (1)] [RreqId (4)]
 */
data class RreqPayload(
    val destId: ByteArray,
    val destSeqNum: UInt,
    val origId: ByteArray,
    val origSeqNum: UInt,
    val hopCount: UByte,
    val rreqId: UInt
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(45).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(destId)
        buffer.putInt(destSeqNum.toInt())
        buffer.put(origId)
        buffer.putInt(origSeqNum.toInt())
        buffer.put(hopCount.toByte())
        buffer.putInt(rreqId.toInt())
        return buffer.array()
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): RreqPayload {
            require(bytes.size >= 45) { "Invalid RREQ payload size" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val destId = ByteArray(16)
            buffer.get(destId)
            val destSeqNum = buffer.getInt().toUInt()
            val origId = ByteArray(16)
            buffer.get(origId)
            val origSeqNum = buffer.getInt().toUInt()
            val hopCount = buffer.get().toUByte()
            val rreqId = buffer.getInt().toUInt()
            
            return RreqPayload(destId, destSeqNum, origId, origSeqNum, hopCount, rreqId)
        }
    }
}

/**
 * Handles creation and parsing of Route Request (RREQ) packets.
 */
@Singleton
class RouteRequestManager @Inject constructor(
    private val packetFactory: PacketFactory,
    private val seqNumManager: SequenceNumberManager
) {
    private val rreqIdCounter = AtomicInteger(1)

    /**
     * Generates a new RREQ packet originating from this node.
     */
    fun createRreq(myId: ByteArray, targetDestId: ByteArray, knownDestSeqNum: UInt): Packet {
        val payload = RreqPayload(
            destId = targetDestId,
            destSeqNum = knownDestSeqNum,
            origId = myId,
            origSeqNum = seqNumManager.incrementAndGet(),
            hopCount = 0u,
            rreqId = rreqIdCounter.incrementAndGet().toUInt()
        )
        
        return packetFactory.createRouteRequest(
            senderNodeId = myId,
            payload = payload.toByteArray()
        )
    }
    
    /**
     * Generates a forwarded RREQ packet.
     */
    fun createForwardedRreq(myId: ByteArray, originalPacket: Packet, parsedPayload: RreqPayload): Packet {
        val forwardedPayload = parsedPayload.copy(
            hopCount = (parsedPayload.hopCount + 1u).toUByte()
        )
        return packetFactory.createRouteRequest(
            senderNodeId = myId,
            payload = forwardedPayload.toByteArray()
        )
    }

    fun parsePayload(payloadBytes: ByteArray): RreqPayload {
        return RreqPayload.fromByteArray(payloadBytes)
    }
}
