package com.bmtp.app.security_hardening

open class SecurityException(message: String, cause: Throwable? = null) : Exception(message, cause)

class RateLimitExceededException(message: String) : SecurityException(message)

class ReplayDetectedException(message: String) : SecurityException(message)

class ProtocolViolationException(message: String) : SecurityException(message)

class CapabilityMismatchException(message: String) : SecurityException(message)

class VersionMismatchException(message: String) : SecurityException(message)

class TrustPolicyException(message: String) : SecurityException(message)

class QuarantineException(message: String) : SecurityException(message)

class SecurityPolicyException(message: String) : SecurityException(message)
