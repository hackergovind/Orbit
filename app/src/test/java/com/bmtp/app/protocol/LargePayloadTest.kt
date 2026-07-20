package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Test

class LargePayloadTest {

    private val factory = PacketFactoryImpl()
    private val serializer = PacketSerializerImpl()
    private val parser = PacketParserImpl()

    @Test
    fun `max payload can be serialized and parsed`() {
        val payload = ByteArray(ProtocolConstants.MAX_PAYLOAD_SIZE_BYTES) { it.toByte() }
        val packet = factory.createMessage(ByteArray(16), ByteArray(16), payload)
        
        val bytes = serializer.serialize(packet)
        assertEquals(ProtocolConstants.MAX_PACKET_SIZE_BYTES, bytes.size)
        
        val parsed = parser.parse(bytes)
        assertArrayEquals(payload, parsed.payload)
    }

    @Test(expected = PacketTooLargeException::class)
    fun `payload slightly above max throws exception at factory`() {
        val payload = ByteArray(ProtocolConstants.MAX_PAYLOAD_SIZE_BYTES + 1)
        factory.createMessage(ByteArray(16), ByteArray(16), payload)
    }
}
