package com.bmtp.app.filetransfer

import java.util.UUID

/**
 * Core metadata describing a file being transferred.
 */
data class FileMetadata(
    /** Unique ID for this specific transfer attempt. */
    val transferId: String = UUID.randomUUID().toString(),
    
    /** The original name of the file. */
    val fileName: String,
    
    /** The total size of the file in bytes (before compression). */
    val fileSizeBytes: Long,
    
    /** MIME type of the file. */
    val mimeType: String = "application/octet-stream",
    
    /** SHA-256 hash of the original complete file for final integrity verification. */
    val fileHash: String,
    
    /** Total number of chunks this file has been split into. */
    val totalChunks: Int,
    
    /** Whether the file contents were compressed before encryption. */
    val isCompressed: Boolean = false,
    
    /** Timestamp when the transfer was initiated. */
    val createdAt: Long = System.currentTimeMillis()
)
