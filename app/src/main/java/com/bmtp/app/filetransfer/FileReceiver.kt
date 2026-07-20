package com.bmtp.app.filetransfer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileReceiver @Inject constructor(
    private val chunkAssembler: ChunkAssembler,
    private val integrityVerifier: IntegrityVerifier,
    private val transferQueue: TransferQueue,
    private val logger: TransferLogger,
    private val stats: TransferStatistics
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    // Phase 8 mock for Phase 6 Decryptor
    private val decryptorMock = object : ChunkDecryptor {
        override fun decrypt(sessionKey: ByteArray, encryptedData: ByteArray): ByteArray {
            // Placeholder: Assume data is decrypted
            return encryptedData
        }
    }

    // Temporary session key map for Phase 8 demonstration
    private val sessionKeys = mutableMapOf<String, ByteArray>()

    /**
     * Called when ReliableTransport delivers an assembled ByteArray.
     * Expects payload format from FileSender: [TransferID (36)] [ChunkIndex (4)] [Length (4)] [Hash (64)] [Payload]
     */
    fun onChunkReceived(rawPayload: ByteArray) {
        scope.launch {
            try {
                // 1. Deserialize Chunk (Mock logic)
                val transferId = String(rawPayload.copyOfRange(0, 36))
                val chunkIndex = byteArrayToInt(rawPayload.copyOfRange(36, 40))
                val length = byteArrayToInt(rawPayload.copyOfRange(40, 44))
                val hash = String(rawPayload.copyOfRange(44, 108))
                val encryptedPayload = rawPayload.copyOfRange(108, rawPayload.size)

                val chunkMetadata = ChunkMetadata(transferId, chunkIndex, length, hash)
                
                val session = transferQueue.getSession(transferId)
                if (session == null) {
                    logger.logError("Received chunk for unknown transfer: $transferId", null)
                    return@launch
                }

                // Retrieve session key
                val sessionKey = sessionKeys.computeIfAbsent(transferId) { ByteArray(32) { 1 } }

                // 2. Decrypt & Assemble & Verify (all done inside assembler for efficiency)
                // For strict verification as requested, we decrypt first, then verify, then assemble
                val decryptedPayload = decryptorMock.decrypt(sessionKey, encryptedPayload)
                
                // Integrity Check (Phase 8 Requirement: "Reject corrupted chunks")
                integrityVerifier.verifyChunk(decryptedPayload, chunkMetadata)

                // 3. Write to Disk
                chunkAssembler.writeChunk(session.metadata, chunkMetadata, encryptedPayload, sessionKey, decryptorMock)

                // 4. Update state
                session.markChunkCompleted(chunkIndex, length)
                logger.logChunkReceived(transferId, chunkIndex)
                stats.recordChunkReceived(length)

                // 5. Check if complete
                if (session.status.get() == TransferStatus.COMPLETED) {
                    finalizeTransfer(session)
                }

            } catch (e: ChunkCorruptedException) {
                // Phase 7 ReliableTransport handles retry if we don't ACK. 
                // We just drop it here.
                logger.logError("Dropped corrupted chunk", e)
                stats.recordDroppedChunk()
            } catch (e: Exception) {
                logger.logError("Failed to process received chunk", e)
            }
        }
    }

    private suspend fun finalizeTransfer(session: TransferSession) {
        try {
            // 1. Move to final destination
            val finalFile = chunkAssembler.finalizeFile(session.metadata)
            
            // 2. Full file hash check (Phase 8 Requirement: "Verify final file hash before completion")
            integrityVerifier.verifyFile(finalFile, session.metadata)
            
            logger.logTransferCompleted(session.metadata.transferId, session.metadata.fileName)
            stats.recordTransferCompleted()
            transferQueue.completeTransfer(session.metadata.transferId)
            
        } catch (e: Exception) {
            logger.logTransferFailed(session.metadata.transferId, "Finalization failed", e)
            transferQueue.failTransfer(session.metadata.transferId)
        }
    }

    private fun byteArrayToInt(bytes: ByteArray): Int {
        return (bytes[0].toInt() and 0xFF shl 24) or
               (bytes[1].toInt() and 0xFF shl 16) or
               (bytes[2].toInt() and 0xFF shl 8) or
               (bytes[3].toInt() and 0xFF)
    }
}
