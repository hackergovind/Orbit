package com.bmtp.app.filetransfer

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles SHA-256 hashing for chunk integrity and full file validation.
 */
@Singleton
class HashManager @Inject constructor(
    private val config: TransferConfig
) {

    /**
     * Hashes a byte array payload in memory (for individual chunks).
     */
    fun hashBytes(payload: ByteArray, length: Int = payload.size): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(payload, 0, length)
        return bytesToHex(digest.digest())
    }

    /**
     * Hashes an entire file efficiently by streaming it through a buffer.
     * Prevents loading the whole file into memory.
     */
    fun hashFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(config.ioBufferSize)
        
        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return bytesToHex(digest.digest())
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
