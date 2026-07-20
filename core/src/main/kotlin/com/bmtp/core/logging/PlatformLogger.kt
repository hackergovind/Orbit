package com.bmtp.core.logging

/**
 * Abstracts logging away from `android.util.Log` or `java.util.logging`.
 * This allows the Core module to run natively on Desktop, CLI, Android, and iOS.
 *
 * Implementations are provided by the platform layer.
 */
interface PlatformLogger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * A simple standard output logger, mostly useful for CLI or Desktop if no framework is provided.
 */
class StdOutLogger : PlatformLogger {
    override fun v(tag: String, message: String) = println("VERBOSE: [$tag] $message")
    override fun d(tag: String, message: String) = println("DEBUG: [$tag] $message")
    override fun i(tag: String, message: String) = println("INFO: [$tag] $message")
    override fun w(tag: String, message: String, throwable: Throwable?) {
        println("WARN: [$tag] $message")
        throwable?.printStackTrace()
    }
    override fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("ERROR: [$tag] $message")
        throwable?.printStackTrace()
    }
}
