package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Test

class TtlTest {

    private val factory = PacketFactoryImpl()

    @Test
    fun `withDecrementedTtl reduces ttl by one`() {
        val original = factory.createPing(ByteArray(16), ByteArray(16))
        val expectedTtl = (original.header.ttl - 1u).toUByte()
        
        val newPacket = original.withDecrementedTtl()
        
        assertEquals(expectedTtl, newPacket.header.ttl)
        // Original should be unchanged
        assertEquals(ProtocolConstants.DEFAULT_TTL, original.header.ttl)
    }

    @Test
    fun `withDecrementedTtl on zero ttl returns same packet`() {
        val original = factory.createPing(ByteArray(16), ByteArray(16))
        val zeroTtlHeader = original.header.copy(ttl = 0u)
        val zeroTtlPacket = original.copy(header = zeroTtlHeader)
        
        val newPacket = zeroTtlPacket.withDecrementedTtl()
        
        assertEquals(0u.toUByte(), newPacket.header.ttl)
        assertTrue(newPacket === zeroTtlPacket) // Should be same instance
    }

    @Test
    fun `withIncrementedHopCount increases count by one`() {
        val original = factory.createPing(ByteArray(16), ByteArray(16))
        val expectedHops = (original.header.hopCount + 1u).toUByte()
        
        val newPacket = original.withIncrementedHopCount()
        
        assertEquals(expectedHops, newPacket.header.hopCount)
        assertEquals(0u.toUByte(), original.header.hopCount)
    }
}
