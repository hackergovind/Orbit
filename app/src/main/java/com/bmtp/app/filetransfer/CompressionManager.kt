package com.bmtp.app.filetransfer

import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles optional compression for chunks.
 * We compress before encrypting, but skip already compressed formats like JPEG or MP4.
 */
@Singleton
class CompressionManager @Inject constructor(
    private val config: TransferConfig,
    private val stats: TransferStatistics
) {
    private val nonCompressibleMimes = setOf(
        "image/jpeg", "image/png", "image/webp",
        "video/mp4", "video/webm",
        "audio/mpeg", "audio/ogg",
        "application/zip", "application/x-7z-compressed"
    )

    fun isCompressible(mimeType: String, fileSizeBytes: Long): Boolean {
        if (fileSizeBytes < config.compressionThresholdBytes) return false
        return !nonCompressibleMimes.contains(mimeType)
    }

    /**
     * Compresses a payload using Deflate.
     */
    fun compress(payload: ByteArray, length: Int = payload.size): ByteArray {
        val bos = ByteArrayOutputStream()
        DeflaterOutputStream(bos).use { dos ->
            dos.write(payload, 0, length)
        }
        val compressed = bos.toByteArray()
        
        // If compression actually inflated the data, return original
        if (compressed.size >= length) {
            return payload.copyOfRange(0, length)
        }
        
        val ratio = compressed.size.toFloat() / length.toFloat()
        stats.updateCompressionRatio(ratio)
        
        return compressed
    }

    /**
     * Decompresses a payload.
     */
    fun decompress(payload: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        InflaterOutputStream(bos).use { ios ->
            ios.write(payload)
        }
        return bos.toByteArray()
    }
}
