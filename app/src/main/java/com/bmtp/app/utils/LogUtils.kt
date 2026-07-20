package com.bmtp.app.utils

import android.util.Log

/**
 * Centralized logging utility for the BMTP application.
 */
object LogUtils {
    private const val TAG = "BMTP"

    /**
     * Logs a debug message.
     *
     * @param subtag The specific module or class name.
     * @param message The message to log.
     */
    fun d(subtag: String, message: String) {
        Log.d("$TAG/$subtag", message)
    }

    /**
     * Logs an informational message.
     *
     * @param subtag The specific module or class name.
     * @param message The message to log.
     */
    fun i(subtag: String, message: String) {
        Log.i("$TAG/$subtag", message)
    }

    /**
     * Logs a warning message.
     *
     * @param subtag The specific module or class name.
     * @param message The message to log.
     */
    fun w(subtag: String, message: String) {
        Log.w("$TAG/$subtag", message)
    }

    /**
     * Logs an error message, optionally with an exception.
     *
     * @param subtag The specific module or class name.
     * @param message The error message to log.
     * @param throwable An optional exception to include in the log.
     */
    fun e(subtag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG/$subtag", message, throwable)
        } else {
            Log.e("$TAG/$subtag", message)
        }
    }
}
