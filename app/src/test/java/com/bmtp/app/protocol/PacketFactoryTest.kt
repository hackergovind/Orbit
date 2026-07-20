package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PacketFactoryTest {

    private lateinit factory: PacketFactoryImpl
    private val senderId = ByteArray(16) { 1 }
    private val receiverId = ByteArray(16) { 2 }

    @Before
    fun setup() {
        factory = PacketFactoryImpl()
    }

    @Test
    fun `createHello generates valid broadcast packet`() {
        val payload = "Info".toByteArray()
        val packet = factory.createHello(senderId, payload)
        
        assertEquals(PacketType.HELLO, packet.header.type)
        assertArrayEquals(ProtocolConstants.BROADCAST_NODE_ID, packet.header.receiverNodeId)
        assertArrayEquals(senderId, packet.header.senderNodeId)
        assertArrayEquals(payload, packet.payload)
        assertTrue(packet.isBroadcast)
    }

    @Test
    fun `createMessage generates correct flags`() {
        val payload = "Hello".toByteArray()
        val packet = factory.createMessage(senderId, receiverId, payload, requiresAck = true)
        
        assertEquals(PacketType.MESSAGE, packet.header.type)
        assertTrue(packet.header.flags.requiresAck)
        assertArrayEquals(payload, packet.payload)
    }

    @Test
    fun `packet ID is always 16 bytes and randomly generated`() {
        val packet1 = factory.createPing(senderId, receiverId)
        val packet2 = factory.createPing(senderId, receiverId)
        
        assertEquals(16, packet1.header.packetId.size)
        assertEquals(16, packet2.header.packetId.size)
        assertFalse(packet1.header.packetId.contentEquals(packet2.header.packetId))
    }
}
