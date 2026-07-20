package com.bmtp.app.filetransfer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles reconstructing interrupted transfers on application startup.
 */
@Singleton
class ResumeManager @Inject constructor(
    private val persistence: TransferPersistence,
    private val transferQueue: TransferQueue,
    private val config: TransferConfig,
    private val logger: TransferLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun initialize() {
        scope.launch {
            try {
                val sessions = persistence.loadAllSessions()
                val currentTime = System.currentTimeMillis()

                for (session in sessions) {
                    val status = session.status.get()
                    
                    // Cleanup stale or failed
                    if (status == TransferStatus.FAILED || status == TransferStatus.CANCELLED) {
                        persistence.deleteSession(session.metadata.transferId)
                        continue
                    }
                    
                    val age = currentTime - session.metadata.createdAt
                    if (age > config.transferStaleTimeoutMs && status != TransferStatus.COMPLETED) {
                        logger.logError("Cleaning up stale transfer: ${session.metadata.transferId}", null)
                        persistence.deleteSession(session.metadata.transferId)
                        continue
                    }

                    // Resume interrupted transfers
                    if (status == TransferStatus.IN_PROGRESS || status == TransferStatus.PENDING) {
                        session.status.set(TransferStatus.PAUSED)
                        transferQueue.enqueue(session, QueuePriority.LOW)
                        transferQueue.resumeTransfer(session.metadata.transferId)
                        logger.logTransferResumed(session.metadata.transferId)
                    }
                }
            } catch (e: Exception) {
                logger.logError("Failed to initialize ResumeManager", e)
            }
        }
    }
}
