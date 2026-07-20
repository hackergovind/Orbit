package com.bmtp.app.models

/**
 * Represents a discovered Bluetooth Mesh device in the network.
 *
 * @property deviceId The unique identifier of the device.
 * @property deviceName The optional name of the device.
 * @property rssi The Received Signal Strength Indicator.
 * @property lastSeenTimestamp The timestamp when the device was last seen (in milliseconds).
 * @property macAddress The MAC address of the device.
 * @property isConnectable Indicates if the device allows connections.
 */
data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String?,
    val rssi: Int,
    val lastSeenTimestamp: Long,
    val macAddress: String,
    val isConnectable: Boolean
) {
    companion object {
        /**
         * Checks if the device has not been seen within the given timeout period.
         *
         * @param device The device to check.
         * @param timeoutMs The timeout duration in milliseconds.
         * @param currentTimeMs The current time in milliseconds.
         * @return `true` if the device is stale, `false` otherwise.
         */
        fun isStale(device: DiscoveredDevice, timeoutMs: Long, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
            return (currentTimeMs - device.lastSeenTimestamp) > timeoutMs
        }
    }
}
