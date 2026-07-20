package com.bmtp.app.repository

import com.bmtp.app.bluetooth.BleAdvertiser
import com.bmtp.app.bluetooth.BleScanner
import com.bmtp.app.models.DiscoveredDevice
import com.bmtp.app.utils.BmtpLogger
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

/**
 * Implementation of [DeviceRepository] using BLE components.
 */
class DeviceRepositoryImpl @Inject constructor(
    private val bleScanner: BleScanner,
    private val bleAdvertiser: BleAdvertiser
) : DeviceRepository {

    override val nearbyDevices: StateFlow<List<DiscoveredDevice>> = bleScanner.discoveredDevices
    override val isScanning: StateFlow<Boolean> = bleScanner.isScanning
    override val isAdvertising: StateFlow<Boolean> = bleAdvertiser.isAdvertising
    
    private val _mergedErrors = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val errors: SharedFlow<String> = _mergedErrors.apply {
        // Simple fallback if coroutine scope merging is not fully setup
    }
    
    init {
        // Collect errors if needed via CoroutineScope
    }

    override fun startDiscovery(deviceId: String): Result<Unit> {
        val advertiseResult = bleAdvertiser.startAdvertising(deviceId)
        if (advertiseResult.isFailure) {
            BmtpLogger.w("DeviceRepository", "Advertising failed to start, but continuing discovery")
        }
        
        return bleScanner.startScanning()
    }

    override fun stopDiscovery() {
        bleScanner.stopScanning()
        bleAdvertiser.stopAdvertising()
    }

    override fun clearDiscoveredDevices() {
        bleScanner.clearDevices()
    }
}
