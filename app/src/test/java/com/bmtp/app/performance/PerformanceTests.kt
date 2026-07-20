package com.bmtp.app.performance

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PerformanceTests {

    private lateinit var config: PerformanceConfig
    private lateinit var logger: PerformanceLogger
    private lateinit var metricsCollector: MetricsCollector

    @Before
    fun setup() {
        config = PerformanceConfig()
        logger = PerformanceLogger()
        metricsCollector = MetricsCollector(logger)
    }

    @Test
    fun `CacheManager - Evicts oldest entries when capacity reached`() {
        // Use a small cache for testing
        val testConfig = PerformanceConfig(maxCacheEntries = 3)
        val cacheManager = CacheManager(testConfig)
        
        cacheManager.packetCache.put("A", ByteArray(10))
        cacheManager.packetCache.put("B", ByteArray(10))
        cacheManager.packetCache.put("C", ByteArray(10))
        
        assertEquals(3, cacheManager.packetCache.size())
        
        // Pushing a 4th should evict "A" (the oldest)
        cacheManager.packetCache.put("D", ByteArray(10))
        
        assertEquals(3, cacheManager.packetCache.size())
        assertNull(cacheManager.packetCache.get("A"))
        assertNotNull(cacheManager.packetCache.get("B"))
    }

    @Test
    fun `CacheManager - trimToSize halves the cache`() {
        val cacheManager = CacheManager(config)
        
        for (i in 1..10) {
            cacheManager.packetCache.put("Key-$i", ByteArray(10))
        }
        
        assertEquals(10, cacheManager.packetCache.size())
        
        cacheManager.trimToSize()
        
        assertEquals(5, cacheManager.packetCache.size())
    }

    @Test
    fun `RelayOptimizer - Critical battery returns score 0`() {
        val relayOptimizer = RelayOptimizer(config, metricsCollector, logger)
        
        metricsCollector.updateBattery(percent = 0.10f, isCharging = false, isCritical = true)
        
        val score = relayOptimizer.evaluateRelayCapacity()
        
        assertEquals(0.0f, score, 0.01f)
    }

    @Test
    fun `RelayOptimizer - Healthy device returns high score`() {
        val relayOptimizer = RelayOptimizer(config, metricsCollector, logger)
        
        metricsCollector.updateBattery(percent = 1.0f, isCharging = true, isCritical = false)
        metricsCollector.updateCpu(10f) // Low CPU
        metricsCollector.updateMeshMetrics(1.0f, 10f, 6, 0) // Lots of neighbors, no queue
        
        val score = relayOptimizer.evaluateRelayCapacity()
        
        // Starts at 1.0, gets +0.1 for having >3 neighbors, clamps to 1.0
        assertEquals(1.0f, score, 0.01f)
    }

    @Test
    fun `ObjectPool - Reuses objects`() {
        var createCount = 0
        val pool = ObjectPool(
            maxSize = 2,
            factory = { 
                createCount++
                ByteArray(10) 
            },
            resetter = { it.fill(0) }
        )
        
        // Borrow 1
        val arr1 = pool.acquire()
        assertEquals(1, createCount)
        
        // Return 1
        arr1[0] = 5
        pool.release(arr1)
        
        // Borrow again
        val arr2 = pool.acquire()
        assertEquals(1, createCount) // Factory not called again
        assertSame(arr1, arr2) // Same instance
        assertEquals(0.toByte(), arr2[0]) // It was reset
    }

    @Test
    fun `QueueOptimizer - Prunes expired packets`() {
        val queueOptimizer = QueueOptimizer(config, logger, metricsCollector)
        
        val now = System.currentTimeMillis()
        val expiredTime = now - config.maxQueueAgeMs - 1000L // 1 second past expiry
        
        queueOptimizer.addMockPacket(expiredTime) // Should be pruned
        queueOptimizer.addMockPacket(now)         // Should stay
        
        queueOptimizer.pruneExpiredPackets()
        
        val metrics = metricsCollector.metrics.value
        assertEquals(1, metrics.queuedPacketsCount) // Only 1 should remain
    }
}
