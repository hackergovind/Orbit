package com.bmtp.app.filetransfer

import com.bmtp.app.transport.ReliableTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * The main facade for the Phase 8 Distributed File Transfer Engine.
 * Provides the application layer with a simple API to start, pause, resume, and cancel transfers.
 */
@Singleton
class FileTransferManager @Inject constructor(
    private val config: TransferConfig,
    private val hashManager: HashManager,
    private val compressionManager: CompressionManager,
    private val transferQueue: TransferQueue,
    private val chunkScheduler: ChunkScheduler,
    private val resumeManager: ResumeManager,
    private val reliableTransport: ReliableTransport,
    private val fileReceiver: FileReceiver,
    private val logger: TransferLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startEngine() {
        resumeManager.initialize()
        chunkScheduler.start()

        // Hook into Phase 7 ReliableTransport to receive incoming chunks
        reliableTransport.onPacketReceived = { packet ->
            // Assume the ReliableTransport gives us the decrypted payload 
            // format: [TransferID (36)] [ChunkIndex (4)] [Length (4)] [Hash (64)] [Payload]
            fileReceiver.onChunkReceived(packet.payload)
        }
    }

    fun stopEngine() {
        chunkScheduler.stop()
    }

    /**
     * Initiates a new file transfer to a remote peer.
     * @param file The file to send.
     * @param remotePeerIdHex The hex ID of the destination node.
     * @param priority The queue priority for this transfer.
     * @return The UUID of the transfer session.
     */
    fun sendFile(
        file: File,
        remotePeerIdHex: String,
        priority: QueuePriority = QueuePriority.NORMAL
    ): String {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        
        val fileSizeBytes = file.length()
        
        // This blocks to calculate hash. In a real UI, this should be moved to a background worker
        val fileHash = hashManager.hashFile(file)
        
        // Determine mime type (simplified)
        val mimeType = "application/octet-stream"
        
        val totalChunks = ceil(fileSizeBytes.toDouble() / config.chunkSizeBytes).toInt()
        val isCompressed = compressionManager.isCompressible(mimeType, fileSizeBytes)
        
        val metadata = FileMetadata(
            fileName = file.name,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            fileHash = fileHash,
            totalChunks = totalChunks,
            isCompressed = isCompressed
        )
        
        val session = TransferSession(metadata, isSender = true, remotePeerIdHex = remotePeerIdHex)
        transferQueue.enqueue(session, priority)
        
        return metadata.transferId
    }

    fun pauseTransfer(transferId: String) {
        transferQueue.pauseTransfer(transferId)
    }

    fun resumeTransfer(transferId: String) {
        transferQueue.resumeTransfer(transferId)
    }

    fun cancelTransfer(transferId: String) {
        transferQueue.cancelTransfer(transferId)
    }
    
    fun getActiveTransfers(): List<TransferSession> {
        return transferQueue.getActiveTransfers()
    }
}
