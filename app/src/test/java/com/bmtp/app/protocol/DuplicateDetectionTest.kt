package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Test

class DuplicateDetectionTest {

    private val cache = PacketCacheImpl()
    private val validator = PacketValidatorImpl(cache)
    private val factory = PacketFactoryImpl()

    @Test(expected = DuplicatePacketException::class)
    fun `identical packets rejected by validator`() {
        val packet = factory.createPing(ByteArray(16), ByteArray(16))
        
        validator.validate(packet) // First one succeeds
        validator.validate(packet) // Second one fails
    }

    @Test
    fun `different packets accepted`() {
        val packet1 = factory.createPing(ByteArray(16), ByteArray(16))
        val packet2 = factory.createPing(ByteArray(16), ByteArray(16)) // Has unique ID
        
        validator.validate(packet1) 
        validator.validate(packet2) 
    }
}
