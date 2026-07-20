package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class PacketParserTest {

    private lateinit serializer: PacketSerializerImpl
    private lateinit parser: PacketParserImpl
    private lateinit factory: PacketFactoryImpl

    @Before
    fun setup() {
        serializer = PacketSerializerImpl()
        parser = PacketParserImpl()
        factory = PacketFactoryImpl()
    }

    @Test
    fun `parse valid packet`() {
        val payload = "Hello world".toByteArray()
        val original = factory.createMessage(ByteArray(16), ByteArray(16), payload)
        
        val serialized = serializer.serialize(original)
        val parsed = parser.parse(serialized)
        
        assertEquals(original.header.packetIdAsString(), parsed.header.packetIdAsString())
        assertArrayEquals(original.payload, parsed.payload)
    }

    @Test(expected = MalformedPacketException::class)
    fun `parse truncated packet throws exception`() {
        val payload = "Hello world".toByteArray()
        val original = factory.createMessage(ByteArray(16), ByteArray(16), payload)
        val serialized = serializer.serialize(original)
        
        val truncated = serialized.copyOfRange(0, 50)
        parser.parse(truncated)
    }

    @Test(expected = ChecksumException::class)
    fun `parse corrupted packet throws exception`() {
        val payload = "Hello world".toByteArray()
        val original = factory.createMessage(ByteArray(16), ByteArray(16), payload)
        val serialized = serializer.serialize(original)
        
        // Corrupt a byte in the payload
        serialized[ProtocolConstants.HEADER_SIZE_BYTES] = 0x00
        
        parser.parse(serialized)
    }
}
