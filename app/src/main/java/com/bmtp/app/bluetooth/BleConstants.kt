package com.bmtp.app.bluetooth

import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import java.util.UUID

/**
 * Defines constants used throughout the Bluetooth Low Energy (BLE) Mesh implementation.
 */
object BleConstants {
    /**
     * Primary UUID for the BMTP Mesh Service.
     */
    val BMTP_SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")

    /**
     * UUID for the characteristic that holds the Device ID.
     */
    val BMTP_DEVICE_ID_CHAR_UUID: UUID = UUID.fromString("b2c3d4e5-f678-90ab-cdef-1234567890ab")

    /**
     * Defines the scanning period. A value of `0L` indicates continuous scanning.
     */
    const val SCAN_PERIOD_MS: Long = 0L

    /**
     * Delay before reporting scan results in milliseconds.
     */
    const val SCAN_REPORT_DELAY_MS: Long = 500L

    /**
     * Timeout duration after which a device is considered stale (in milliseconds).
     */
    const val DEVICE_STALE_TIMEOUT_MS: Long = 30_000L

    /**
     * Interval for cleaning up stale devices from the list (in milliseconds).
     */
    const val STALE_CLEANUP_INTERVAL_MS: Long = 10_000L

    /**
     * Advertisement mode for BLE broadcasting.
     */
    const val ADVERTISE_MODE: Int = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY

    /**
     * Transmission power for BLE broadcasting.
     */
    const val ADVERTISE_TX_POWER: Int = AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM

    /**
     * Notification channel ID for the foreground service.
     */
    const val NOTIFICATION_CHANNEL_ID: String = "bmtp_mesh_channel"

    /**
     * Notification ID for the foreground service.
     */
    const val NOTIFICATION_ID: Int = 1001

    /**
     * ParcelUuid representation of the service UUID, used in advertisement data.
     */
    val SERVICE_DATA_DEVICE_ID_KEY: ParcelUuid = ParcelUuid(BMTP_SERVICE_UUID)
}
