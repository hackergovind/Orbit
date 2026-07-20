package com.bmtp.app.bluetooth

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles Bluetooth Low Energy advertising to peers.
 */
interface BleAdvertiser {
    /**
     * Flow indicating whether advertising is currently active.
     */
    val isAdvertising: StateFlow<Boolean>

    /**
     * Flow emitting advertising errors.
     */
    val advertiseErrors: SharedFlow<String>

    /**
     * Starts advertising the device over BLE.
     * @param deviceId The unique identifier of this device to advertise.
     * @return Result indicating success or failure.
     */
    fun startAdvertising(deviceId: String): Result<Unit>

    /**
     * Stops advertising the device over BLE.
     */
    fun stopAdvertising()
}
