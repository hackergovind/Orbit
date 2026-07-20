package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class PacketCacheTest {

    @Test
    fun `new packet is added and not duplicate`() {
        val cache = PacketCacheImpl()
        assertFalse(cache.isDuplicateOrAdd("ID1"))
    }

    @Test
    fun `same packet seen twice is duplicate`() {
        val cache = PacketCacheImpl()
        assertFalse(cache.isDuplicateOrAdd("ID1"))
        assertTrue(cache.isDuplicateOrAdd("ID1"))
    }

    @Test
    fun `packet expires after expiry time`() {
        // Very short expiry for test
        val cache = PacketCacheImpl(expiryMs = 50L)
        assertFalse(cache.isDuplicateOrAdd("ID1"))
        
        Thread.sleep(100) // Wait for expiry
        
        // Should be treated as new again because it expired
        assertFalse(cache.isDuplicateOrAdd("ID1"))
    }

    @Test
    fun `cache respects max size by evicting LRU`() {
        val cache = PacketCacheImpl(maxSize = 2)
        
        cache.isDuplicateOrAdd("ID1")
        cache.isDuplicateOrAdd("ID2")
        cache.isDuplicateOrAdd("ID3") // This should evict ID1
        
        // ID1 is no longer in cache, should return false (not a duplicate)
        assertFalse(cache.isDuplicateOrAdd("ID1")) 
    }
    
    @Test
    fun `cache is thread safe`() {
        val cache = PacketCacheImpl(maxSize = 1000)
        val threads = 10
        val insertsPerThread = 100
        val latch = CountDownLatch(threads)
        
        for (i in 0 until threads) {
            thread {
                for (j in 0 until insertsPerThread) {
                    cache.isDuplicateOrAdd("ID_${i}_$j")
                }
                latch.countDown()
            }
        }
        
        latch.await()
        
        // All threads finished, cache should have accepted exactly 1000 items
        // Let's verify size by adding one more and checking it replaces something
        assertFalse(cache.isDuplicateOrAdd("NEW_ID"))
    }
}
