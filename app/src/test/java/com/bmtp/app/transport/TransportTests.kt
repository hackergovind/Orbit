package com.bmtp.app.transport

import com.bmtp.app.protocol.Packet
import com.bmtp.app.protocol.PacketFactoryImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class TransportTests {

    private lateinit var config: TransportConfig
    private lateinit var sessionState: SessionState
    private lateinit var logger: TransportLogger
    private lateinit var sequenceManager: SequenceManager
    private lateinit var duplicateDetector: DuplicateDetector
    private lateinit var congestionController: CongestionController
    private lateinit var stats: TransportStatistics

    @Before
    fun setup() {
        config = TransportConfig(duplicateCacheSize = 10, duplicateExpiryMs = 1000L)
        stats = TransportStatistics()
        logger = TransportLogger()
        sessionState = SessionState()
        sequenceManager = SequenceManager(sessionState, config, logger)
        duplicateDetector = DuplicateDetector(config, stats, logger)
        congestionController = CongestionController(config, logger)
    }

    @Test
    fun `DuplicateDetector drops recently seen packets`() {
        val packetIdHex = "aabbccdd"
        
        assertFalse(duplicateDetector.isDuplicate(packetIdHex))
        assertTrue(duplicateDetector.isDuplicate(packetIdHex))
    }

    @Test
    fun `DuplicateDetector accepts packets after expiry`() {
        val shortExpiryConfig = TransportConfig(duplicateCacheSize = 10, duplicateExpiryMs = 10L)
        val detector = DuplicateDetector(shortExpiryConfig, stats, logger)
        val packetIdHex = "11223344"
        
        assertFalse(detector.isDuplicate(packetIdHex))
        Thread.sleep(50) // Wait for expiry
        assertFalse(detector.isDuplicate(packetIdHex))
    }

    @Test
    fun `SequenceManager orders packets correctly`() {
        val sender = "sender1"
        val packetFactory = PacketFactoryImpl()
        
        // Dummy packets
        val p1 = packetFactory.createPing(ByteArray(16), ByteArray(16))
        val p2 = packetFactory.createPing(ByteArray(16), ByteArray(16))
        val p3 = packetFactory.createPing(ByteArray(16), ByteArray(16))

        // Process p2 (out of order, arrives early)
        val ready1 = sequenceManager.processIncomingPacket(sender, 2u, p2)
        assertTrue(ready1.isEmpty())
        
        // Process p3 (out of order, arrives early)
        val ready2 = sequenceManager.processIncomingPacket(sender, 3u, p3)
        assertTrue(ready2.isEmpty())
        
        // Process p1 (in order)
        val ready3 = sequenceManager.processIncomingPacket(sender, 1u, p1)
        
        // It should return p1, p2, p3 in order
        assertEquals(3, ready3.size)
        assertEquals(p1.header.packetId.toList(), ready3[0].header.packetId.toList())
        assertEquals(p2.header.packetId.toList(), ready3[1].header.packetId.toList())
        assertEquals(p3.header.packetId.toList(), ready3[2].header.packetId.toList())
        
        assertEquals(4u, sessionState.expectedIncomingSequence(sender))
    }

    @Test
    fun `CongestionController additive increase and multiplicative decrease`() {
        val peer = "peerA"
        
        // Initial window is 10
        assertTrue(congestionController.canSend(peer))
        
        // Send 10 packets
        for (i in 1..10) congestionController.onPacketSent(peer)
        
        // Window is full
        assertFalse(congestionController.canSend(peer))
        
        // 1 ACK comes back -> Window grows by 1 (to 11), in-flight drops to 9
        congestionController.onAckReceived(peer)
        assertTrue(congestionController.canSend(peer))
        
        // Simulate packet loss -> Window halves to 5
        congestionController.onPacketLoss(peer)
        assertFalse(congestionController.canSend(peer)) // In-flight is 8, window is 5
    }
}
