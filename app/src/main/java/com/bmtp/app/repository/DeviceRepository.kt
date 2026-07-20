package com.bmtp.app.repository

import com.bmtp.app.models.DiscoveredDevice
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing device discovery and presence.
 */
interface DeviceRepository {
    /**
     * Flow of nearby discovered devices.
     */
    val nearbyDevices: StateFlow<List<DiscoveredDevice>>

    /**
     * True if currently scanning for devices.
     */
    val isScanning: StateFlow<Boolean>

    /**
     * True if currently advertising presence.
     */
    val isAdvertising: StateFlow<Boolean>

    /**
     * Flow of errors from underlying BLE operations.
     */
    val errors: SharedFlow<String>

    /**
     * Starts discovering peers and advertising this device.
     * @param deviceId The local device identifier.
     * @return Result indicating if discovery started successfully.
     */
    fun startDiscovery(deviceId: String): Result<Unit>

    /**
     * Stops discovering peers and advertising.
     */
    fun stopDiscovery()

    /**
     * Clears the list of known discovered devices.
     */
    fun clearDiscoveredDevices()
}
