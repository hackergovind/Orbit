package com.bmtp.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import com.bmtp.app.utils.BmtpLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Implementation of [BleAdvertiser].
 */
@SuppressLint("MissingPermission")
class BleAdvertiserImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BleAdvertiser {

    private val _isAdvertising = MutableStateFlow(false)
    override val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _advertiseErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val advertiseErrors: SharedFlow<String> = _advertiseErrors.asSharedFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            BmtpLogger.i("BleAdvertiserImpl", "Started advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            val errorMsg = mapAdvertiseFailureCode(errorCode)
            BmtpLogger.e("BleAdvertiserImpl", "Advertise failed: $errorMsg")
            _advertiseErrors.tryEmit("Advertise failed: $errorMsg")
            _isAdvertising.value = false
        }
    }

    override fun startAdvertising(deviceId: String): Result<Unit> {
        if (_isAdvertising.value) return Result.success(Unit)

        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val advertiser = bluetoothManager?.adapter?.bluetoothLeAdvertiser
                ?: return Result.failure(Exception("BLE advertising not supported"))

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(BleConstants.ADVERTISE_MODE)
                .setTxPowerLevel(BleConstants.ADVERTISE_TX_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .build()

            val deviceIdBytes = hexStringToByteArray(deviceId)
            
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(BleConstants.BMTP_SERVICE_UUID))
                .addServiceData(ParcelUuid(BleConstants.BMTP_SERVICE_UUID), deviceIdBytes)
                .build()

            advertiser.startAdvertising(settings, data, advertiseCallback)
            return Result.success(Unit)
        } catch (e: SecurityException) {
            BmtpLogger.e("BleAdvertiserImpl", "SecurityException starting advertise", e)
            return Result.failure(e)
        } catch (e: Exception) {
            BmtpLogger.e("BleAdvertiserImpl", "Exception starting advertise", e)
            return Result.failure(e)
        }
    }

    override fun stopAdvertising() {
        if (!_isAdvertising.value) return
        
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothManager?.adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            BmtpLogger.e("BleAdvertiserImpl", "SecurityException stopping advertise", e)
        } catch (e: Exception) {
            BmtpLogger.e("BleAdvertiserImpl", "Exception stopping advertise", e)
        } finally {
            _isAdvertising.value = false
            BmtpLogger.i("BleAdvertiserImpl", "Stopped advertising")
        }
    }

    private fun mapAdvertiseFailureCode(errorCode: Int): String {
        return when (errorCode) {
            AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
            AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
            AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
            AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
            else -> "Unknown error code: $errorCode"
        }
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
