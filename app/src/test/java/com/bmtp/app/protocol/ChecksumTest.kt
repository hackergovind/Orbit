package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Test
import java.util.zip.CRC32

class ChecksumTest {
    
    private val factory = PacketFactoryImpl()
    private val serializer = PacketSerializerImpl()
    private val parser = PacketParserImpl()

    @Test
    fun `checksum validation passes for valid packet`() {
        val packet = factory.createPing(ByteArray(16), ByteArray(16))
        val bytes = serializer.serialize(packet)
        val parsed = parser.parse(bytes)
        
        // No ChecksumException thrown
        assertNotNull(parsed)
    }

    @Test(expected = ChecksumException::class)
    fun `single bit flip causes checksum failure`() {
        val packet = factory.createPing(ByteArray(16), ByteArray(16))
        val bytes = serializer.serialize(packet)
        
        // Flip a bit in the header
        bytes[10] = (bytes[10].toInt() xor 0x01).toByte()
        
        parser.parse(bytes)
    }
}
