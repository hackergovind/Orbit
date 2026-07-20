package com.bmtp.app.filetransfer

import com.bmtp.app.transport.ReliableTransport
import com.bmtp.app.utils.HexUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSender @Inject constructor(
    private val reliableTransport: ReliableTransport,
    private val fileChunker: FileChunker,
    private val config: TransferConfig,
    private val logger: TransferLogger,
    private val stats: TransferStatistics,
    private val chunkCache: ChunkCache
) {
    // In Phase 8 we mock the encryptor dependency from Phase 6.
    private val encryptorMock = object : ChunkEncryptor {
        override fun encrypt(sessionKey: ByteArray, data: ByteArray): ByteArray {
            // Placeholder: Assume data is encrypted
            return data
        }
    }

    // Temporary session key map for Phase 8 demonstration
    private val sessionKeys = mutableMapOf<String, ByteArray>()

    suspend fun sendChunk(session: TransferSession, chunkIndex: Int) = withContext(Dispatchers.IO) {
        val transferId = session.metadata.transferId
        val file = File(session.metadata.fileName) // In a real app this would resolve URI/path
        
        // Retrieve or generate session key
        val sessionKey = sessionKeys.computeIfAbsent(transferId) { ByteArray(32) { 1 } }
        
        try {
            // Check cache first to avoid re-reading from disk on retries
            var encryptedChunk = chunkCache.get(transferId, chunkIndex)
            
            if (encryptedChunk == null) {
                // Not in cache, stream from disk
                encryptedChunk = fileChunker.createChunk(
                    file = file,
                    metadata = session.metadata,
                    chunkIndex = chunkIndex,
                    sessionKey = sessionKey,
                    encryptor = encryptorMock
                )
                
                // Cache it while it's in flight
                chunkCache.put(encryptedChunk.metadata, encryptedChunk.payload)
            }

            // Serialize and send via Phase 7 ReliableTransport
            val payloadToTransmit = serializeChunk(encryptedChunk)
            
            val targetNodeId = HexUtils.hexStringToByteArray(session.remotePeerIdHex)
            reliableTransport.send(targetNodeId, payloadToTransmit)
            
            logger.logChunkSent(transferId, chunkIndex)
            stats.recordChunkSent(encryptedChunk.payload.size)
            
        } catch (e: Exception) {
            logger.logError("Failed to send chunk $chunkIndex for transfer $transferId", e)
            throw e
        }
    }

    /**
     * Serializes ChunkMetadata + EncryptedPayload into a single ByteArray for Phase 7
     */
    private fun serializeChunk(chunk: EncryptedChunk): ByteArray {
        // Mock serialization. Real implementation uses Protocol Buffers or DataOutputStream
        // format: [TransferID (36 bytes)] [ChunkIndex (4 bytes)] [Length (4 bytes)] [Hash (64 bytes)] [Payload]
        val payload = chunk.payload
        val result = ByteArray(36 + 4 + 4 + 64 + payload.size)
        // ... serialization logic omitted for brevity in mock
        System.arraycopy(payload, 0, result, 108, payload.size)
        return result
    }

    /**
     * Called when Phase 7 reports an ACK for a specific chunk.
     */
    fun onChunkAckReceived(transferId: String, chunkIndex: Int, session: TransferSession) {
        // We can safely remove it from memory cache now that the receiver has it
        chunkCache.remove(transferId, chunkIndex)
        
        // Update session state
        val chunkSizeBytes = if (chunkIndex == session.metadata.totalChunks - 1) {
             (session.metadata.fileSizeBytes % config.chunkSizeBytes).toInt()
        } else {
             config.chunkSizeBytes
        }
        
        session.markChunkCompleted(chunkIndex, chunkSizeBytes)
    }
}
