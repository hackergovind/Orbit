package com.bmtp.app.utils

/**
 * Utility object for time-related formatting and calculations.
 */
object TimeUtils {

    /**
     * Extension function to convert a timestamp in milliseconds into a relative time string.
     * Formats like "just now", "5s ago", "2m ago", "1h ago", "3d ago".
     *
     * @return A human-readable relative time string.
     */
    fun Long.toRelativeTimeString(): String {
        val now = System.currentTimeMillis()
        val diffSeconds = (now - this) / 1000

        return when {
            diffSeconds < 5 -> "just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            else -> "${diffSeconds / 86400}d ago"
        }
    }
}
