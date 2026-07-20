package com.bmtp.app.filetransfer

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

enum class TransferStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Tracks the live, runtime state of an active file transfer.
 */
class TransferSession(
    val metadata: FileMetadata,
    val isSender: Boolean,
    val remotePeerIdHex: String
) {
    val status = AtomicReference(TransferStatus.PENDING)
    
    // Tracks indices of chunks that have been successfully ACKed (if sender) or received & verified (if receiver)
    private val completedChunks = ConcurrentHashMap.newKeySet<Int>()
    
    // Metrics
    private val bytesTransferred = AtomicLong(0)
    private val startTimeMs = AtomicLong(0)
    private val lastActivityTimeMs = AtomicLong(System.currentTimeMillis())

    fun markStarted() {
        status.compareAndSet(TransferStatus.PENDING, TransferStatus.IN_PROGRESS)
        if (startTimeMs.get() == 0L) {
            startTimeMs.set(System.currentTimeMillis())
        }
        updateActivity()
    }

    fun markChunkCompleted(index: Int, chunkSizeBytes: Int) {
        if (completedChunks.add(index)) {
            bytesTransferred.addAndGet(chunkSizeBytes.toLong())
        }
        updateActivity()
        
        if (completedChunks.size == metadata.totalChunks) {
            status.set(TransferStatus.COMPLETED)
        }
    }

    fun isChunkCompleted(index: Int): Boolean = completedChunks.contains(index)
    
    fun getCompletedChunkIndices(): Set<Int> = completedChunks.toSet()

    fun updateActivity() {
        lastActivityTimeMs.set(System.currentTimeMillis())
    }

    fun getProgressPercentage(): Float {
        if (metadata.totalChunks == 0) return 100f
        return (completedChunks.size.toFloat() / metadata.totalChunks.toFloat()) * 100f
    }

    fun getAverageSpeedBytesPerSec(): Float {
        val elapsed = System.currentTimeMillis() - startTimeMs.get()
        if (elapsed <= 0) return 0f
        return (bytesTransferred.get().toFloat() / elapsed) * 1000f
    }

    fun getMissingChunks(): List<Int> {
        val missing = mutableListOf<Int>()
        for (i in 0 until metadata.totalChunks) {
            if (!completedChunks.contains(i)) {
                missing.add(i)
            }
        }
        return missing
    }

    /** Used by ResumeManager to inject state from disk */
    fun restoreCompletedChunks(indices: Set<Int>, bytes: Long) {
        completedChunks.addAll(indices)
        bytesTransferred.set(bytes)
    }
}
