package com.bmtp.app.filetransfer

/**
 * Configuration parameters for the BMTP Distributed File Transfer Engine.
 */
data class TransferConfig(
    /** Default size of a single file chunk in bytes (e.g., 256 KB). */
    val chunkSizeBytes: Int = 256 * 1024,
    
    /** Maximum number of concurrent file transfers allowed. */
    val maxConcurrentTransfers: Int = 10,
    
    /** Maximum number of times a single chunk will be retried before failing the transfer. */
    val maxChunkRetries: Int = 5,
    
    /** Threshold size in bytes above which a file is considered "large" (e.g., 50 MB). */
    val largeFileThresholdBytes: Long = 50 * 1024 * 1024L,
    
    /** Maximum size of the chunk cache in bytes to prevent OutOfMemory errors (e.g., 50 MB). */
    val maxChunkCacheSizeBytes: Long = 50 * 1024 * 1024L,
    
    /** If a file's size is above this threshold and it's compressible, compression will be attempted. */
    val compressionThresholdBytes: Long = 1024L,
    
    /** Buffer size for I/O operations (e.g., 8 KB). */
    val ioBufferSize: Int = 8192,
    
    /** Directory name for storing temporary/incomplete transfers. */
    val tempDirectoryName: String = ".bmtp_temp",
    
    /** Directory name for storing completed transfers. */
    val completedDirectoryName: String = "bmtp_downloads",
    
    /** Timeout in milliseconds before a paused/inactive transfer is considered stale. */
    val transferStaleTimeoutMs: Long = 24 * 60 * 60 * 1000L // 24 hours
)
