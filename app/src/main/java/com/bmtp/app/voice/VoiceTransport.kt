package com.bmtp.app.voice

import com.bmtp.app.transport.ReliableTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles sending and receiving raw packetized voice arrays over the mesh.
 * Makes the critical decision between Unreliable (Live Voice) and Reliable (Voice Notes).
 */
@Singleton
class VoiceTransport @Inject constructor(
    private val config: VoiceConfig,
    private val reliableTransport: ReliableTransport, // Phase 7
    // private val meshForwarding: MeshForwarding, // Phase 4 (Mocked usage below)
    private val logger: VoiceLogger,
    private val stats: VoiceStatistics
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Callback for when a voice packet is received from the network
    var onVoicePacketReceived: ((ByteArray) -> Unit)? = null

    /**
     * Sends a packetized voice frame over the network.
     */
    fun sendVoicePacket(targetNodeIdHex: String, packet: ByteArray, packetType: VoicePacketType) {
        scope.launch {
            try {
                if (packetType == VoicePacketType.VOICE_NOTE) {
                    // Voice Notes must arrive 100% reliably
                    val targetNodeBytes = targetNodeIdHex.toByteArray() // Mock conversion
                    reliableTransport.send(targetNodeBytes, packet)
                } else {
                    // Live Voice (PTT or Live Call)
                    if (config.useUnreliableTransportForLive) {
                        // In a real integration, this calls Phase 4's MeshForwarding directly
                        // bypassing the Phase 7 ARQ/Retry mechanisms to prevent Head-of-Line blocking.
                        
                        // meshForwarding.sendUnreliable(targetNodeIdHex, packet, ttl = config.voiceBroadcastTtl)
                        
                        // Mocking the call:
                        mockUnreliableSend(targetNodeIdHex, packet)
                    } else {
                        val targetNodeBytes = targetNodeIdHex.toByteArray()
                        reliableTransport.send(targetNodeBytes, packet)
                    }
                }
                
                stats.recordPacketSent(packet.size)
                
            } catch (e: Exception) {
                logger.logError("Failed to send voice packet", e)
            }
        }
    }

    /**
     * Hook called by the lower networking layers when a voice payload arrives.
     */
    fun handleIncomingPacket(payload: ByteArray) {
        stats.recordPacketReceived(payload.size)
        onVoicePacketReceived?.invoke(payload)
    }
    
    private fun mockUnreliableSend(target: String, packet: ByteArray) {
        // Simulates dropping the packet straight onto the BLE mesh without waiting for ACKs
        logger.logPacketSent(target, 0, packet.size) // Seq num mocked for logger here
    }
}
