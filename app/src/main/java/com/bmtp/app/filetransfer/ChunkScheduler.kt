package com.bmtp.app.filetransfer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Iterates through active TransferSessions in the TransferQueue and dispatches
 * requests to the FileSender to generate and transmit the next chunk.
 * Multiplexes multiple file transfers seamlessly.
 */
@Singleton
class ChunkScheduler @Inject constructor(
    private val transferQueue: TransferQueue,
    private val fileSender: FileSender,
    private val logger: TransferLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var schedulerJob: Job? = null

    fun start() {
        if (schedulerJob?.isActive == true) return
        
        schedulerJob = scope.launch {
            while (isActive) {
                val activeTransfers = transferQueue.getActiveTransfers()
                if (activeTransfers.isEmpty()) {
                    delay(100)
                    continue
                }

                // Round-robin dispatching: Give each active transfer a chance to send a chunk
                for (session in activeTransfers) {
                    if (session.status.get() != TransferStatus.IN_PROGRESS) continue

                    // Find next missing chunk
                    val missing = session.getMissingChunks()
                    if (missing.isNotEmpty()) {
                        val nextChunk = missing.first() // Basic sequential scheduling
                        try {
                            fileSender.sendChunk(session, nextChunk)
                            delay(10) // Small yield to prevent CPU hogging and let ReliableTransport process
                        } catch (e: Exception) {
                            logger.logTransferFailed(session.metadata.transferId, "Scheduler error", e)
                            transferQueue.failTransfer(session.metadata.transferId)
                        }
                    } else if (session.status.get() == TransferStatus.COMPLETED) {
                        transferQueue.completeTransfer(session.metadata.transferId)
                    }
                }
            }
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }
}
