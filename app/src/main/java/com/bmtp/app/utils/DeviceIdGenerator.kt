package com.bmtp.app.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Utility class to generate and manage a unique Device ID for the mesh network.
 *
 * @param context The application context.
 */
class DeviceIdGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bmtp_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_DEVICE_ID = "device_id"
    }

    /**
     * Retrieves the existing Device ID or generates a new one if it doesn't exist.
     *
     * @return The unique 16-byte hex string representing the Device ID.
     */
    fun getOrCreateDeviceId(): String {
        return getDeviceId() ?: generateAndSaveDeviceId()
    }

    /**
     * Retrieves the existing Device ID.
     *
     * @return The Device ID string, or `null` if it hasn't been generated yet.
     */
    fun getDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }

    /**
     * Generates a new random 16-byte hex string and saves it to SharedPreferences.
     *
     * @return The newly generated Device ID.
     */
    private fun generateAndSaveDeviceId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        val deviceId = bytes.joinToString("") { "%02x".format(it) }
        
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }
}
