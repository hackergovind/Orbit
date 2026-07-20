package com.bmtp.app.filetransfer

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

enum class QueuePriority {
    HIGH, NORMAL, LOW
}

data class QueuedTransfer(
    val session: TransferSession,
    val priority: QueuePriority = QueuePriority.NORMAL,
    val queuedAt: Long = System.currentTimeMillis()
) : Comparable<QueuedTransfer> {
    override fun compareTo(other: QueuedTransfer): Int {
        val p = this.priority.compareTo(other.priority)
        if (p != 0) return p
        return this.queuedAt.compareTo(other.queuedAt)
    }
}

/**
 * Manages active and pending file transfers to respect concurrency limits.
 */
@Singleton
class TransferQueue @Inject constructor(
    private val config: TransferConfig,
    private val logger: TransferLogger
) {
    private val activeTransfers = ConcurrentHashMap<String, TransferSession>()
    private val pendingQueue = PriorityBlockingQueue<QueuedTransfer>()
    private val pausedTransfers = ConcurrentHashMap<String, TransferSession>()

    fun enqueue(session: TransferSession, priority: QueuePriority = QueuePriority.NORMAL): Boolean {
        if (activeTransfers.size < config.maxConcurrentTransfers) {
            activeTransfers[session.metadata.transferId] = session
            session.markStarted()
            logger.logTransferStarted(session.metadata.transferId, session.metadata.fileName, session.metadata.fileSizeBytes)
            return true
        } else {
            pendingQueue.offer(QueuedTransfer(session, priority))
            logger.logTransferPaused(session.metadata.transferId) // Treat as paused/queued
            return false
        }
    }

    fun pauseTransfer(transferId: String) {
        val session = activeTransfers.remove(transferId)
        if (session != null) {
            session.status.set(TransferStatus.PAUSED)
            pausedTransfers[transferId] = session
            logger.logTransferPaused(transferId)
            promoteNext()
        }
    }

    fun resumeTransfer(transferId: String) {
        val session = pausedTransfers.remove(transferId)
        if (session != null) {
            enqueue(session, QueuePriority.HIGH) // Resumed transfers get priority
        }
    }

    fun completeTransfer(transferId: String) {
        activeTransfers.remove(transferId)
        promoteNext()
    }

    fun failTransfer(transferId: String) {
        val session = activeTransfers.remove(transferId) ?: pendingQueue.find { it.session.metadata.transferId == transferId }?.session
        if (session != null) {
            session.status.set(TransferStatus.FAILED)
        }
        pendingQueue.removeIf { it.session.metadata.transferId == transferId }
        promoteNext()
    }

    fun cancelTransfer(transferId: String) {
        val session = activeTransfers.remove(transferId) ?: pausedTransfers.remove(transferId)
        if (session != null) {
            session.status.set(TransferStatus.CANCELLED)
        }
        pendingQueue.removeIf { it.session.metadata.transferId == transferId }
        promoteNext()
    }

    private fun promoteNext() {
        while (activeTransfers.size < config.maxConcurrentTransfers && pendingQueue.isNotEmpty()) {
            val next = pendingQueue.poll()
            if (next != null) {
                activeTransfers[next.session.metadata.transferId] = next.session
                next.session.markStarted()
                logger.logTransferResumed(next.session.metadata.transferId)
            }
        }
    }

    fun getActiveTransfers(): List<TransferSession> = activeTransfers.values.toList()
    fun getSession(transferId: String): TransferSession? = activeTransfers[transferId] ?: pausedTransfers[transferId]
}
