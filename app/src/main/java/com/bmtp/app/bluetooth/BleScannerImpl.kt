package com.bmtp.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.bmtp.app.models.DiscoveredDevice
import com.bmtp.app.utils.BmtpLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Implementation of [BleScanner].
 */
@SuppressLint("MissingPermission")
class BleScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BleScanner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _discoveredDevicesMap = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevicesMap
        .map { map -> map.values.sortedByDescending { it.rssi } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val scanErrors: SharedFlow<String> = _scanErrors.asSharedFlow()

    private var cleanupJob: Job? = null
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = parseScanResult(result)
            _discoveredDevicesMap.value = _discoveredDevicesMap.value.toMutableMap().apply {
                put(device.deviceId, device)
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            val newMap = _discoveredDevicesMap.value.toMutableMap()
            for (result in results) {
                val device = parseScanResult(result)
                newMap[device.deviceId] = device
            }
            _discoveredDevicesMap.value = newMap
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMsg = mapScanFailureCode(errorCode)
            BmtpLogger.e("BleScannerImpl", "Scan failed: $errorMsg")
            _scanErrors.tryEmit("Scan failed: $errorMsg")
            _isScanning.value = false
        }
    }

    override fun startScanning(): Result<Unit> {
        if (_isScanning.value) return Result.success(Unit)

        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val scanner = bluetoothManager?.adapter?.bluetoothLeScanner
                ?: return Result.failure(Exception("BluetoothLeScanner not available"))

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(BleConstants.SCAN_REPORT_DELAY_MS)
                .build()

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.BMTP_SERVICE_UUID))
                .build()

            scanner.startScan(listOf(filter), settings, scanCallback)
            startStaleDeviceCleanup()
            _isScanning.value = true
            BmtpLogger.i("BleScannerImpl", "Started scanning")
            return Result.success(Unit)
        } catch (e: SecurityException) {
            BmtpLogger.e("BleScannerImpl", "SecurityException starting scan", e)
            return Result.failure(e)
        } catch (e: Exception) {
            BmtpLogger.e("BleScannerImpl", "Exception starting scan", e)
            return Result.failure(e)
        }
    }

    override fun stopScanning() {
        if (!_isScanning.value) return
        
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            BmtpLogger.e("BleScannerImpl", "SecurityException stopping scan", e)
        } catch (e: Exception) {
            BmtpLogger.e("BleScannerImpl", "Exception stopping scan", e)
        } finally {
            cleanupJob?.cancel()
            _isScanning.value = false
            BmtpLogger.i("BleScannerImpl", "Stopped scanning")
        }
    }

    override fun clearDevices() {
        _discoveredDevicesMap.value = emptyMap()
    }

    private fun startStaleDeviceCleanup() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(BleConstants.STALE_CLEANUP_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val filteredMap = _discoveredDevicesMap.value.filter {
                    (now - it.value.lastSeenTimestamp) <= BleConstants.DEVICE_STALE_TIMEOUT_MS
                }
                if (filteredMap.size != _discoveredDevicesMap.value.size) {
                    _discoveredDevicesMap.value = filteredMap
                }
            }
        }
    }

    private fun parseScanResult(result: ScanResult): DiscoveredDevice {
        val serviceData = result.scanRecord?.getServiceData(ParcelUuid(BleConstants.BMTP_SERVICE_UUID))
        
        val deviceId = if (serviceData != null) {
            serviceData.joinToString("") { "%02x".format(it) }
        } else {
            result.device.address
        }
        
        return DiscoveredDevice(
            deviceId = deviceId,
            deviceName = result.scanRecord?.deviceName ?: result.device.name,
            rssi = result.rssi,
            lastSeenTimestamp = System.currentTimeMillis(),
            macAddress = result.device.address,
            isConnectable = result.isConnectable
        )
    }

    private fun mapScanFailureCode(errorCode: Int): String {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Already started"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
            ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Out of hardware resources"
            else -> "Unknown error code: $errorCode"
        }
    }
}
