package com.bmtp.app.filetransfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

data class EncryptedChunk(
    val metadata: ChunkMetadata,
    val payload: ByteArray
)

/**
 * Interface to the Phase 6 Encryption engine (Mocked here since we're only building Phase 8).
 * Real implementation would be provided by Hilt.
 */
interface ChunkEncryptor {
    fun encrypt(sessionKey: ByteArray, data: ByteArray): ByteArray
}

@Singleton
class FileChunker @Inject constructor(
    private val config: TransferConfig,
    private val hashManager: HashManager,
    private val compressionManager: CompressionManager
) {
    /**
     * Reads a specific chunk from a file on disk, optionally compresses it, hashes it,
     * and encrypts it using the provided encryptor.
     * 
     * Uses RandomAccessFile to support O(1) seeking, essential for large files (GBs) and resumability.
     */
    suspend fun createChunk(
        file: File,
        metadata: FileMetadata,
        chunkIndex: Int,
        sessionKey: ByteArray,
        encryptor: ChunkEncryptor
    ): EncryptedChunk = withContext(Dispatchers.IO) {
        val offset = chunkIndex.toLong() * config.chunkSizeBytes
        val bufferSize = if (chunkIndex == metadata.totalChunks - 1) {
            (metadata.fileSizeBytes - offset).toInt()
        } else {
            config.chunkSizeBytes
        }
        
        val rawBuffer = ByteArray(bufferSize)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            raf.readFully(rawBuffer)
        }

        // Process Payload
        val payload = if (metadata.isCompressed) {
            compressionManager.compress(rawBuffer)
        } else {
            rawBuffer
        }

        val chunkHash = hashManager.hashBytes(payload)
        
        val chunkMeta = ChunkMetadata(
            transferId = metadata.transferId,
            chunkIndex = chunkIndex,
            length = payload.size,
            chunkHash = chunkHash
        )

        // Encrypt (Requirement: Do not expose plaintext to relay nodes)
        val encryptedPayload = encryptor.encrypt(sessionKey, payload)

        return@withContext EncryptedChunk(chunkMeta, encryptedPayload)
    }
}
