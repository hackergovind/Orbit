package com.bmtp.app.mesh

import com.bmtp.app.protocol.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class MeshTests {

    private lateinit var stats: MeshStatistics
    private lateinit var config: MeshConfig
    private lateinit var logger: MeshLogger
    private lateinit var packetFactory: PacketFactoryImpl

    @Before
    fun setup() {
        stats = MeshStatistics()
        config = MeshConfig()
        logger = MeshLogger()
        packetFactory = PacketFactoryImpl()
    }

    @Test
    fun `TTLManager drops expired packet`() {
        val ttlManager = TTLManager(stats)
        val expiredHeader = PacketHeader(
            version = ProtocolVersion.V1,
            type = PacketType.PING,
            flags = PacketFlags(0u),
            ttl = 0u,
            hopCount = 0u,
            packetId = ByteArray(16),
            senderNodeId = ByteArray(16),
            receiverNodeId = ByteArray(16),
            payloadLength = 0u
        )
        val packet = Packet(expiredHeader, ByteArray(0), 0u)

        assertThrows(TtlExpiredException::class.java) {
            ttlManager.decrementTtl(packet)
        }
        assertEquals(1, stats.metrics.value.ttlExpired)
    }

    @Test
    fun `HopManager drops packet exceeding max hops`() {
        val hopManager = HopManager(config)
        
        val header = PacketHeader(
            version = ProtocolVersion.V1,
            type = PacketType.PING,
            flags = PacketFlags(0u),
            ttl = 10u,
            hopCount = 15u, // config.maxHopCount is 15
            packetId = ByteArray(16),
            senderNodeId = ByteArray(16),
            receiverNodeId = ByteArray(16),
            payloadLength = 0u
        )
        val packet = Packet(header, ByteArray(0), 0u)

        assertThrows(HopLimitExceededException::class.java) {
            hopManager.incrementHopCount(packet)
        }
    }

    @Test
    fun `ForwardQueue evicts oldest packet when full`() {
        val smallConfig = config.copy(forwardQueueSize = 2)
        val queue = ForwardQueue(smallConfig, logger, stats)
        
        val packet1 = packetFactory.createPing(ByteArray(16), ByteArray(16))
        val packet2 = packetFactory.createPing(ByteArray(16), ByteArray(16))
        val packet3 = packetFactory.createPing(ByteArray(16), ByteArray(16))
        
        queue.enqueue(packet1)
        queue.enqueue(packet2)
        
        // This should evict packet1
        queue.enqueue(packet3)
        
        val valid = queue.dequeueAllValid()
        assertEquals(2, valid.size)
        assertTrue(valid.contains(packet2))
        assertTrue(valid.contains(packet3))
        assertFalse(valid.contains(packet1))
        
        assertEquals(1, stats.metrics.value.packetsDropped)
    }

    @Test
    fun `ControlledFloodingPolicy excludes source node`() {
        val policy = ControlledFloodingPolicy()
        val neighbors = setOf("nodeA", "nodeB", "nodeC")
        
        val packet = packetFactory.createPing(ByteArray(16), ByteArray(16))
        
        val targets = policy.selectTargets(packet, neighbors, "nodeB")
        
        assertEquals(2, targets.size)
        assertTrue(targets.contains("nodeA"))
        assertTrue(targets.contains("nodeC"))
        assertFalse(targets.contains("nodeB"))
    }
}
