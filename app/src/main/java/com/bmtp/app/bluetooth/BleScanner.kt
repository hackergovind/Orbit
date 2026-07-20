package com.bmtp.app.bluetooth

import com.bmtp.app.models.DiscoveredDevice
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles Bluetooth Low Energy scanning for peers.
 */
interface BleScanner {
    /**
     * Flow of currently discovered devices, sorted by signal strength (RSSI).
     */
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>

    /**
     * Flow indicating whether scanning is currently active.
     */
    val isScanning: StateFlow<Boolean>

    /**
     * Flow emitting scan errors.
     */
    val scanErrors: SharedFlow<String>

    /**
     * Starts scanning for BLE devices.
     * @return Result indicating success or failure.
     */
    fun startScanning(): Result<Unit>

    /**
     * Stops scanning for BLE devices.
     */
    fun stopScanning()

    /**
     * Clears the list of currently discovered devices.
     */
    fun clearDevices()
}
