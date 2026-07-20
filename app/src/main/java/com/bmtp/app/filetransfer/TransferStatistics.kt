package com.bmtp.app.filetransfer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class FileTransferMetrics(
    val completedTransfers: Long = 0,
    val cancelledTransfers: Long = 0,
    val failedTransfers: Long = 0,
    val chunksSent: Long = 0,
    val chunksReceived: Long = 0,
    val droppedChunks: Long = 0,
    val duplicateChunks: Long = 0,
    val chunkRetries: Long = 0,
    val integrityFailures: Long = 0,
    val totalBytesTransferred: Long = 0,
    val compressionRatioAvg: Float = 1.0f,
    val averageThroughputBps: Float = 0f
)

@Singleton
class TransferStatistics @Inject constructor() {
    private val _metrics = MutableStateFlow(FileTransferMetrics())
    val metrics: StateFlow<FileTransferMetrics> = _metrics.asStateFlow()

    fun recordTransferCompleted() = _metrics.update { it.copy(completedTransfers = it.completedTransfers + 1) }
    fun recordTransferCancelled() = _metrics.update { it.copy(cancelledTransfers = it.cancelledTransfers + 1) }
    fun recordTransferFailed() = _metrics.update { it.copy(failedTransfers = it.failedTransfers + 1) }
    
    fun recordChunkSent(bytes: Int) = _metrics.update { 
        it.copy(
            chunksSent = it.chunksSent + 1,
            totalBytesTransferred = it.totalBytesTransferred + bytes
        ) 
    }
    
    fun recordChunkReceived(bytes: Int) = _metrics.update { 
        it.copy(
            chunksReceived = it.chunksReceived + 1,
            totalBytesTransferred = it.totalBytesTransferred + bytes
        ) 
    }
    
    fun recordDroppedChunk() = _metrics.update { it.copy(droppedChunks = it.droppedChunks + 1) }
    fun recordDuplicateChunk() = _metrics.update { it.copy(duplicateChunks = it.duplicateChunks + 1) }
    fun recordRetry() = _metrics.update { it.copy(chunkRetries = it.chunkRetries + 1) }
    fun recordIntegrityFailure() = _metrics.update { it.copy(integrityFailures = it.integrityFailures + 1) }
    
    fun updateCompressionRatio(ratio: Float) = _metrics.update {
        // Very basic moving average for demonstration
        val newAvg = if (it.compressionRatioAvg == 1.0f) ratio else (it.compressionRatioAvg + ratio) / 2f
        it.copy(compressionRatioAvg = newAvg)
    }
}
