package com.bmtp.app.performance

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * A lock-free Object Pool to reuse heavy allocations (like ByteArray buffers)
 * and prevent GC churn leading to CPU spikes and audio stutter.
 */
class ObjectPool<T>(
    private val maxSize: Int,
    private val factory: () -> T,
    private val resetter: (T) -> Unit
) {
    // ConcurrentLinkedQueue avoids lock contention under heavy multi-threaded load
    private val pool = ConcurrentLinkedQueue<T>()
    private val currentSize = AtomicInteger(0)

    /**
     * Borrows an object from the pool, or creates a new one if the pool is empty.
     */
    fun acquire(): T {
        val obj = pool.poll()
        if (obj != null) {
            currentSize.decrementAndGet()
            return obj
        }
        return factory()
    }

    /**
     * Returns an object to the pool, resetting its state.
     * If the pool is full, the object is discarded to GC.
     */
    fun release(instance: T) {
        if (currentSize.get() < maxSize) {
            resetter(instance)
            pool.offer(instance)
            currentSize.incrementAndGet()
        }
    }
    
    fun size(): Int = currentSize.get()
}
