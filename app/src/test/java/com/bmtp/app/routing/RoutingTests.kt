package com.bmtp.app.routing

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class RoutingTests {

    private lateinit var config: RoutingConfig
    private lateinit var logger: RoutingLogger
    private lateinit var stats: RoutingStatistics
    private lateinit var selector: RouteSelector
    private lateinit var table: RoutingTable
    private lateinit var seqNumManager: SequenceNumberManager

    @Before
    fun setup() {
        config = RoutingConfig()
        logger = RoutingLogger()
        stats = RoutingStatistics()
        selector = RouteSelector()
        table = RoutingTable(selector, logger, stats, config)
        seqNumManager = SequenceNumberManager()
    }

    @Test
    fun `RouteSelector prefers higher sequence number`() {
        val existing = RouteEntry("dest1", "hop1", 2u, 100u, 0)
        val candidate = RouteEntry("dest1", "hop2", 5u, 101u, 0) // Worse hops, but better seq num
        
        assertTrue(selector.isCandidateBetter(existing, candidate))
    }

    @Test
    fun `RouteSelector prefers lower hop count if sequence number equal`() {
        val existing = RouteEntry("dest1", "hop1", 3u, 100u, 0)
        val candidate = RouteEntry("dest1", "hop2", 2u, 100u, 0) // Same seq num, better hops
        
        assertTrue(selector.isCandidateBetter(existing, candidate))
    }
    
    @Test
    fun `RouteSelector rejects worse candidate`() {
        val existing = RouteEntry("dest1", "hop1", 2u, 100u, 0)
        val candidate = RouteEntry("dest1", "hop2", 5u, 100u, 0) // Same seq num, worse hops
        
        assertFalse(selector.isCandidateBetter(existing, candidate))
    }

    @Test
    fun `RoutingTable adds new route`() {
        val entry = RouteEntry("dest1", "hop1", 1u, 1u, System.currentTimeMillis() + 1000)
        assertTrue(table.addOrUpdateRoute(entry))
        assertNotNull(table.getActiveRoute("dest1"))
    }

    @Test
    fun `RoutingTable invalidates route and increments sequence number`() {
        val entry = RouteEntry("dest1", "hop1", 1u, 100u, System.currentTimeMillis() + 1000)
        table.addOrUpdateRoute(entry)
        
        table.invalidateRoute("dest1")
        
        // Should no longer be active
        assertNull(table.getActiveRoute("dest1"))
        
        // But should still exist as invalid with incremented seq num
        val invalidRoute = table.getAnyRoute("dest1")
        assertNotNull(invalidRoute)
        assertEquals(RouteStatus.INVALID, invalidRoute!!.status)
        assertEquals(101u, invalidRoute.sequenceNumber)
    }

    @Test
    fun `RoutingTable evicts expired routes`() {
        val expiredTime = System.currentTimeMillis() - 1000
        val entry = RouteEntry("dest1", "hop1", 1u, 1u, expiredTime)
        
        table.addOrUpdateRoute(entry)
        
        // Actively fetching it will still return it until evicted or if logic in getActive checks expiry (our current implementation relies on the sweeper)
        
        // Run eviction
        table.evictExpiredRoutes(System.currentTimeMillis())
        
        assertNull(table.getAnyRoute("dest1"))
    }

    @Test
    fun `SequenceNumberManager increments safely`() {
        val initial = seqNumManager.getCurrent()
        val next = seqNumManager.incrementAndGet()
        assertEquals(initial + 1u, next)
    }
    
    @Test
    fun `RreqPayload serializes and parses correctly`() {
        val origPayload = RreqPayload(
            destId = ByteArray(16) { 1 },
            destSeqNum = 500u,
            origId = ByteArray(16) { 2 },
            origSeqNum = 100u,
            hopCount = 3u,
            rreqId = 999u
        )
        
        val bytes = origPayload.toByteArray()
        val parsed = RreqPayload.fromByteArray(bytes)
        
        assertArrayEquals(origPayload.destId, parsed.destId)
        assertEquals(origPayload.destSeqNum, parsed.destSeqNum)
        assertArrayEquals(origPayload.origId, parsed.origId)
        assertEquals(origPayload.origSeqNum, parsed.origSeqNum)
        assertEquals(origPayload.hopCount, parsed.hopCount)
        assertEquals(origPayload.rreqId, parsed.rreqId)
    }
}
