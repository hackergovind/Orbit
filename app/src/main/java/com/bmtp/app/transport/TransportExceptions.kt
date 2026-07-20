package com.bmtp.app.transport

open class TransportException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AckTimeoutException(message: String) : TransportException(message)

class RetryLimitExceededException(message: String) : TransportException(message)

class QueueOverflowException(message: String) : TransportException(message)

class SequenceViolationException(message: String) : TransportException(message)

class DuplicateDeliveryException(message: String) : TransportException(message)

class PacketExpiredException(message: String) : TransportException(message)

class TransportFailureException(message: String, cause: Throwable? = null) : TransportException(message, cause)
