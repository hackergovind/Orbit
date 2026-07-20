package com.bmtp.app.voice

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VoiceTests {

    private lateinit var config: VoiceConfig
    private lateinit var logger: VoiceLogger
    private lateinit var stats: VoiceStatistics

    @Before
    fun setup() {
        config = VoiceConfig()
        logger = VoiceLogger()
        stats = VoiceStatistics()
    }

    @Test
    fun `Jitter Buffer - Reorders out of order packets`() {
        val jitterBuffer = JitterBuffer(config, stats, logger)
        
        val frame1 = createMockFrame(1)
        val frame2 = createMockFrame(2)
        val frame3 = createMockFrame(3)
        
        // Push out of order
        jitterBuffer.push(frame3)
        jitterBuffer.push(frame1)
        jitterBuffer.push(frame2)
        
        // Need to push enough to exit initial buffering
        for (i in 4..6) {
            jitterBuffer.push(createMockFrame(i))
        }

        // Pop should be ordered
        assertEquals(1, jitterBuffer.pop()?.sequenceNumber)
        assertEquals(2, jitterBuffer.pop()?.sequenceNumber)
        assertEquals(3, jitterBuffer.pop()?.sequenceNumber)
    }
    
    @Test
    fun `Jitter Buffer - Drops excessively late packets`() {
        val jitterBuffer = JitterBuffer(config, stats, logger)
        
        for (i in 1..6) {
            jitterBuffer.push(createMockFrame(i))
        }
        
        assertEquals(1, jitterBuffer.pop()?.sequenceNumber)
        assertEquals(2, jitterBuffer.pop()?.sequenceNumber)
        
        // Now next expected is 3. Try pushing 1 (very late)
        val lateFrame = createMockFrame(1)
        jitterBuffer.push(lateFrame)
        
        // The stats should register a drop
        assertEquals(1L, stats.metrics.value.totalPacketsDropped)
    }

    @Test
    fun `Voice Scheduler - Downgrades bitrate on high packet loss`() {
        val scheduler = VoiceScheduler(config, stats, logger)
        
        assertEquals(16000, scheduler.currentBitrateBps.value)
        
        // Simulate severe network degradation
        stats.updateNetworkQuality(latencyMs = 250f, lossRate = 0.20f) // 20% loss
        
        scheduler.evaluateNetworkConditions()
        
        // Should drop to 70% of 16000 = 11200
        assertEquals(11200, scheduler.currentBitrateBps.value)
    }

    @Test
    fun `Voice Encryption - Throws on tampered packet`() {
        val encryption = VoiceEncryption(logger)
        val sessionKey = ByteArray(32) { 0x01 }
        
        val originalData = ByteArray(10) { 0x55 }
        val encrypted = encryption.encrypt(sessionKey, originalData)
        
        // Tamper with the packet by injecting the mock auth failure byte
        val tampered = encrypted.clone()
        tampered[0] = 0xDE.toByte()
        
        assertThrows(VoiceEncryptionException::class.java) {
            encryption.decrypt(sessionKey, tampered)
        }
    }

    private fun createMockFrame(seq: Int): EncodedVoiceFrame {
        return EncodedVoiceFrame(
            sessionId = "test",
            sequenceNumber = seq,
            timestamp = System.currentTimeMillis(),
            packetType = VoicePacketType.LIVE_VOICE,
            codecId = 1,
            bitrate = 16000,
            payload = ByteArray(10)
        )
    }
}
