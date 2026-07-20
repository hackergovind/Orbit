package com.bmtp.app.filetransfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrityVerifier @Inject constructor(
    private val hashManager: HashManager,
    private val logger: TransferLogger,
    private val stats: TransferStatistics
) {
    /**
     * Verifies that the payload of a single chunk matches the hash reported in its metadata.
     * Note: the payload here is assumed to be the *unencrypted* (and potentially decompressed)
     * byte array as the metadata hash is computed on the original file chunk.
     */
    fun verifyChunk(payload: ByteArray, metadata: ChunkMetadata) {
        val actualHash = hashManager.hashBytes(payload)
        if (actualHash != metadata.chunkHash) {
            logger.logIntegrityFailure(metadata.transferId, metadata.chunkIndex, metadata.chunkHash, actualHash)
            stats.recordIntegrityFailure()
            throw ChunkCorruptedException("Hash mismatch for chunk ${metadata.chunkIndex}")
        }
        logger.logChunkVerified(metadata.transferId, metadata.chunkIndex)
    }

    /**
     * Verifies that the final assembled file on disk matches the original file hash
     * provided in the FileMetadata.
     */
    suspend fun verifyFile(file: File, metadata: FileMetadata) = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw StorageFailureException("Assembled file not found for verification")
        }
        
        if (file.length() != metadata.fileSizeBytes) {
            val msg = "File size mismatch. Expected ${metadata.fileSizeBytes}, got ${file.length()}"
            logger.logError(msg, null)
            stats.recordIntegrityFailure()
            throw IntegrityCheckFailedException(msg)
        }
        
        val actualHash = hashManager.hashFile(file)
        if (actualHash != metadata.fileHash) {
            val msg = "Final file hash mismatch. Expected ${metadata.fileHash}, got $actualHash"
            logger.logError(msg, null)
            stats.recordIntegrityFailure()
            throw IntegrityCheckFailedException(msg)
        }
    }
}
