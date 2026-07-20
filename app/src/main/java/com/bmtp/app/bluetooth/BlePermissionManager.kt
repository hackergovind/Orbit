package com.bmtp.app.bluetooth

/**
 * Manages Bluetooth and Location permissions required for BLE operations.
 */
interface BlePermissionManager {
    /**
     * Checks if all required permissions have been granted.
     * @return true if all permissions are granted, false otherwise.
     */
    fun hasRequiredPermissions(): Boolean

    /**
     * Retrieves the list of required permissions based on the current Android API level.
     * @return List of permission strings.
     */
    fun getRequiredPermissions(): List<String>

    /**
     * Checks if Bluetooth is currently enabled on the device.
     * @return true if Bluetooth is enabled, false otherwise.
     */
    fun isBluetoothEnabled(): Boolean

    /**
     * Checks if Bluetooth Low Energy (BLE) is supported on the device.
     * @return true if BLE is supported, false otherwise.
     */
    fun isBluetoothSupported(): Boolean
}
