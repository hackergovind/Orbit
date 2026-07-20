package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PacketSerializerTest {

    private lateinit serializer: PacketSerializerImpl
    private lateinit factory: PacketFactoryImpl

    @Before
    fun setup() {
        serializer = PacketSerializerImpl()
        factory = PacketFactoryImpl()
    }

    @Test
    fun `serialize packet correctly`() {
        val senderId = ByteArray(16) { 1 }
        val receiverId = ByteArray(16) { 2 }
        val payload = "Hello".toByteArray()
        
        val packet = factory.createMessage(senderId, receiverId, payload)
        val serialized = serializer.serialize(packet)
        
        assertEquals(ProtocolConstants.HEADER_SIZE_BYTES + payload.size + ProtocolConstants.FOOTER_SIZE_BYTES, serialized.size)
        
        val buffer = ByteBuffer.wrap(serialized).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(ProtocolVersion.V1.byteValue.toByte(), buffer.get())
        assertEquals(PacketType.MESSAGE.byteValue.toByte(), buffer.get())
    }

    @Test
    fun `serialize max payload`() {
        val payload = ByteArray(ProtocolConstants.MAX_PAYLOAD_SIZE_BYTES)
        val packet = factory.createBroadcast(ByteArray(16), payload)
        val serialized = serializer.serialize(packet)
        assertEquals(ProtocolConstants.MAX_PACKET_SIZE_BYTES, serialized.size)
    }

    @Test(expected = PacketTooLargeException::class)
    fun `serialize too large payload throws exception`() {
        val payload = ByteArray(ProtocolConstants.MAX_PAYLOAD_SIZE_BYTES + 1)
        val packet = factory.createBroadcast(ByteArray(16), payload)
        serializer.serialize(packet)
    }
}
