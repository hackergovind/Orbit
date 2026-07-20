package com.bmtp.app.filetransfer

/**
 * Identifies a specific chunk of a file.
 * The payload itself is handled separately to keep this lightweight.
 */
data class ChunkMetadata(
    /** The UUID of the file transfer this chunk belongs to. */
    val transferId: String,
    
    /** The index of this chunk (0-based). */
    val chunkIndex: Int,
    
    /** The size of the payload in this chunk. Will match config chunk size except for the final chunk. */
    val length: Int,
    
    /** SHA-256 hash of the chunk's *unencrypted* payload for integrity validation. */
    val chunkHash: String
)
