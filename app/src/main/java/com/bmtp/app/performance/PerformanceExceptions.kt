package com.bmtp.app.performance

open class PerformanceException(message: String, cause: Throwable? = null) : Exception(message, cause)

class MemoryPressureException(message: String) : PerformanceException(message)

class ResourceLimitException(message: String) : PerformanceException(message)

class QueueStarvationException(message: String) : PerformanceException(message)

class OptimizationFailureException(message: String, cause: Throwable? = null) : PerformanceException(message, cause)

class BatteryCriticalException(message: String) : PerformanceException(message)

class BenchmarkFailureException(message: String, cause: Throwable? = null) : PerformanceException(message, cause)
