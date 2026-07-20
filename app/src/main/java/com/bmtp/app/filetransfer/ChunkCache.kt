package com.bmtp.app.filetransfer

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A fast memory-bounded cache for chunks currently being processed.
 * Prevents OutOfMemoryErrors by strictly limiting memory usage.
 */
@Singleton
class ChunkCache @Inject constructor(
    config: TransferConfig,
    private val logger: TransferLogger
) {
    // Key format: "transferId:chunkIndex"
    private val cache = object : LruCache<String, EncryptedChunk>(
        // Size in bytes
        (config.maxChunkCacheSizeBytes).toInt() 
    ) {
        override fun sizeOf(key: String, value: EncryptedChunk): Int {
            return value.payload.size
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: EncryptedChunk, newValue: EncryptedChunk?) {
            if (evicted) {
                // We don't log directly for every single eviction to avoid spam, but this is where it happens.
                // In a highly concurrent environment, chunks will flow through disk.
            }
        }
    }

    fun put(metadata: ChunkMetadata, encryptedPayload: ByteArray) {
        val key = "${metadata.transferId}:${metadata.chunkIndex}"
        cache.put(key, EncryptedChunk(metadata, encryptedPayload))
    }

    fun get(transferId: String, chunkIndex: Int): EncryptedChunk? {
        val key = "$transferId:$chunkIndex"
        return cache.get(key)
    }

    fun remove(transferId: String, chunkIndex: Int) {
        val key = "$transferId:$chunkIndex"
        cache.remove(key)
    }

    fun clearTransfer(transferId: String) {
        // Not perfectly efficient, but LruCache doesn't support bulk removal by prefix easily.
        // We iterate and remove.
        val keysToRemove = cache.snapshot().keys.filter { it.startsWith("$transferId:") }
        keysToRemove.forEach { cache.remove(it) }
    }
}
