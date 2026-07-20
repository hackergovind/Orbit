package com.bmtp.app.filetransfer

open class FileTransferException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ChunkMissingException(message: String) : FileTransferException(message)

class TransferInterruptedException(message: String) : FileTransferException(message)

class IntegrityCheckFailedException(message: String) : FileTransferException(message)

class ChunkCorruptedException(message: String) : FileTransferException(message)

class ResumeFailureException(message: String, cause: Throwable? = null) : FileTransferException(message, cause)

class StorageFailureException(message: String, cause: Throwable? = null) : FileTransferException(message, cause)

class TransferCancelledException(message: String) : FileTransferException(message)

class TransferTimeoutException(message: String) : FileTransferException(message)

class InvalidMetadataException(message: String) : FileTransferException(message)
