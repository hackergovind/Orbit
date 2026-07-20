package com.bmtp.app.utils

/**
 * Utility object for interpreting RSSI (Received Signal Strength Indicator) values.
 */
object RssiUtils {

    /**
     * Converts an RSSI value into a discrete signal level (0 to 4).
     *
     * @param rssi The RSSI value in dBm.
     * @return A signal level where 4 is the strongest and 0 is the weakest.
     */
    fun rssiToSignalLevel(rssi: Int): Int {
        return when {
            rssi >= -50 -> 4
            rssi >= -60 -> 3
            rssi >= -70 -> 2
            rssi >= -80 -> 1
            else -> 0
        }
    }

    /**
     * Estimates the physical distance based on the RSSI value.
     *
     * @param rssi The RSSI value in dBm.
     * @return A string representing a rough distance estimate.
     */
    fun rssiToDistanceEstimate(rssi: Int): String {
        return when {
            rssi >= -50 -> "Very close (<1m)"
            rssi >= -60 -> "Close (1-3m)"
            rssi >= -70 -> "Medium (3-7m)"
            rssi >= -80 -> "Far (7-15m)"
            else -> "Very far (>15m)"
        }
    }

    /**
     * Provides a descriptive string for signal quality based on RSSI.
     *
     * @param rssi The RSSI value in dBm.
     * @return A string describing the signal quality ("Excellent", "Good", etc.).
     */
    fun rssiToSignalQuality(rssi: Int): String {
        return when {
            rssi >= -50 -> "Excellent"
            rssi >= -60 -> "Good"
            rssi >= -70 -> "Fair"
            rssi >= -80 -> "Weak"
            else -> "Very Weak"
        }
    }
}
