package com.bmtp.app.filetransfer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface to the Phase 6 Decryption engine.
 */
interface ChunkDecryptor {
    fun decrypt(sessionKey: ByteArray, encryptedData: ByteArray): ByteArray
}

@Singleton
class ChunkAssembler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: TransferConfig,
    private val compressionManager: CompressionManager
) {
    /**
     * Gets or creates the temporary file for an incoming transfer.
     */
    fun getTempFile(transferId: String): File {
        val dir = File(context.cacheDir, config.tempDirectoryName)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$transferId.tmp")
    }

    /**
     * Writes a decrypted, validated chunk to the correct offset in the temporary file.
     * Uses RandomAccessFile for O(1) writing, allowing out-of-order chunk assembly.
     */
    suspend fun writeChunk(
        metadata: FileMetadata,
        chunkMetadata: ChunkMetadata,
        encryptedPayload: ByteArray,
        sessionKey: ByteArray,
        decryptor: ChunkDecryptor
    ) = withContext(Dispatchers.IO) {
        val tempFile = getTempFile(metadata.transferId)
        
        // Decrypt
        val payload = decryptor.decrypt(sessionKey, encryptedPayload)
        
        // Decompress if necessary
        val finalData = if (metadata.isCompressed) {
            compressionManager.decompress(payload)
        } else {
            payload
        }
        
        // Write to exact offset
        val offset = chunkMetadata.chunkIndex.toLong() * config.chunkSizeBytes
        RandomAccessFile(tempFile, "rw").use { raf ->
            raf.seek(offset)
            raf.write(finalData)
        }
    }

    /**
     * Moves the assembled temporary file to the final destination directory.
     */
    suspend fun finalizeFile(metadata: FileMetadata): File = withContext(Dispatchers.IO) {
        val tempFile = getTempFile(metadata.transferId)
        val outDir = File(context.filesDir, config.completedDirectoryName)
        if (!outDir.exists()) outDir.mkdirs()
        
        val finalFile = File(outDir, metadata.fileName)
        tempFile.copyTo(finalFile, overwrite = true)
        tempFile.delete()
        
        return@withContext finalFile
    }
}
