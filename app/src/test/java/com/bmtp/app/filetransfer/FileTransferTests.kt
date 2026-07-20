package com.bmtp.app.filetransfer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class FileTransferTests {

    private lateinit var config: TransferConfig
    private lateinit var hashManager: HashManager
    private lateinit var compressionManager: CompressionManager
    private lateinit var stats: TransferStatistics
    private lateinit var logger: TransferLogger
    private lateinit var integrityVerifier: IntegrityVerifier

    @Before
    fun setup() {
        config = TransferConfig()
        stats = TransferStatistics()
        logger = TransferLogger()
        hashManager = HashManager(config)
        compressionManager = CompressionManager(config, stats)
        integrityVerifier = IntegrityVerifier(hashManager, logger, stats)
    }

    @Test
    fun `Compression reduces size of text payload`() {
        // Create highly compressible payload
        val original = "A".repeat(10000).toByteArray()
        val compressed = compressionManager.compress(original)
        
        assertTrue(compressed.size < original.size)
        
        val decompressed = compressionManager.decompress(compressed)
        assertArrayEquals(original, decompressed)
    }

    @Test
    fun `HashManager generates consistent SHA256 hashes`() {
        val payload = "Hello BMTP".toByteArray()
        val hash1 = hashManager.hashBytes(payload)
        val hash2 = hashManager.hashBytes(payload)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `IntegrityVerifier rejects corrupted chunks`() {
        val payload = "Valid Chunk Data".toByteArray()
        val correctHash = hashManager.hashBytes(payload)
        
        val metadata = ChunkMetadata(
            transferId = UUID.randomUUID().toString(),
            chunkIndex = 0,
            length = payload.size,
            chunkHash = correctHash
        )
        
        // This should pass silently
        integrityVerifier.verifyChunk(payload, metadata)
        
        // Corrupt payload
        val corruptedPayload = "Valid Chunk Data!".toByteArray()
        
        assertThrows(ChunkCorruptedException::class.java) {
            integrityVerifier.verifyChunk(corruptedPayload, metadata)
        }
    }
    
    @Test
    fun `TransferQueue respects concurrency limits`() {
        val queueConfig = TransferConfig(maxConcurrentTransfers = 2)
        val queue = TransferQueue(queueConfig, logger)
        
        val meta1 = FileMetadata(fileName="1", fileSizeBytes=100, fileHash="h", totalChunks=1)
        val meta2 = FileMetadata(fileName="2", fileSizeBytes=100, fileHash="h", totalChunks=1)
        val meta3 = FileMetadata(fileName="3", fileSizeBytes=100, fileHash="h", totalChunks=1)
        
        val s1 = TransferSession(meta1, true, "peer")
        val s2 = TransferSession(meta2, true, "peer")
        val s3 = TransferSession(meta3, true, "peer")
        
        assertTrue(queue.enqueue(s1)) // Starts immediately
        assertTrue(queue.enqueue(s2)) // Starts immediately
        assertFalse(queue.enqueue(s3)) // Queued because limit is 2
        
        assertEquals(2, queue.getActiveTransfers().size)
        
        // Complete one
        queue.completeTransfer(s1.metadata.transferId)
        
        // s3 should now be promoted
        assertEquals(2, queue.getActiveTransfers().size)
        assertTrue(queue.getActiveTransfers().any { it.metadata.transferId == meta3.transferId })
    }
}
