package com.bmtp.app.filetransfer

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized logger for File Transfer events.
 * Crucially adheres to the requirement: "Never log decrypted file contents."
 */
@Singleton
class TransferLogger @Inject constructor() {
    private val subtag = "FileTransferEngine"

    fun logTransferStarted(transferId: String, fileName: String, sizeBytes: Long) {
        LogUtils.i(subtag, "Transfer Started: $transferId | File: $fileName | Size: $sizeBytes bytes")
    }

    fun logTransferPaused(transferId: String) {
        LogUtils.i(subtag, "Transfer Paused: $transferId")
    }

    fun logTransferResumed(transferId: String) {
        LogUtils.i(subtag, "Transfer Resumed: $transferId")
    }

    fun logChunkSent(transferId: String, chunkIndex: Int) {
        LogUtils.v(subtag, "Chunk Sent: $transferId | Index: $chunkIndex")
    }

    fun logChunkReceived(transferId: String, chunkIndex: Int) {
        LogUtils.v(subtag, "Chunk Received: $transferId | Index: $chunkIndex")
    }

    fun logChunkVerified(transferId: String, chunkIndex: Int) {
        LogUtils.v(subtag, "Chunk Verified: $transferId | Index: $chunkIndex")
    }

    fun logChunkRetransmitted(transferId: String, chunkIndex: Int, attempt: Int) {
        LogUtils.w(subtag, "Chunk Retransmitted: $transferId | Index: $chunkIndex | Attempt: $attempt")
    }

    fun logTransferCompleted(transferId: String, fileName: String) {
        LogUtils.i(subtag, "Transfer Completed: $transferId | File: $fileName")
    }

    fun logTransferCancelled(transferId: String) {
        LogUtils.i(subtag, "Transfer Cancelled: $transferId")
    }

    fun logTransferFailed(transferId: String, reason: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, "Transfer Failed: $transferId | Reason: $reason", throwable)
    }
    
    fun logIntegrityFailure(transferId: String, chunkIndex: Int, expectedHash: String, actualHash: String) {
        LogUtils.e(subtag, "Integrity Failure: $transferId | Chunk: $chunkIndex | Expected: $expectedHash | Actual: $actualHash", null)
    }
}
