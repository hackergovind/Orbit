package com.bmtp.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

// BMTP BLE Service UUID — same as BleConstants in the legacy module
private val BMTP_SERVICE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
private val BMTP_MSG_CHAR_UUID = UUID.fromString("b2c3d4e5-f678-90ab-cdef-1234567890ab")

data class BleDiscoveredNode(val id: String, val name: String, val rssi: Int, val mac: String)
data class BleMessage(val senderId: String, val text: String, val isFromMe: Boolean)

/**
 * Singleton BLE transport manager.
 * Handles BLE advertising (making yourself visible) + scanning (discovering others),
 * and GATT server/client for sending messages.
 */
@SuppressLint("MissingPermission")
object BleTransportManager {

    var nodeId: String = ""
    var displayName: String = "Orbit User"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _discoveryFlow = MutableSharedFlow<BleDiscoveredNode>(extraBufferCapacity = 20)
    val discoveryFlow: SharedFlow<BleDiscoveredNode> = _discoveryFlow.asSharedFlow()

    private val _messageFlow = MutableSharedFlow<BleMessage>(extraBufferCapacity = 50)
    val messageFlow: SharedFlow<BleMessage> = _messageFlow.asSharedFlow()

    private var bluetoothManager: BluetoothManager? = null
    private var gattServer: BluetoothGattServer? = null
    private var isRunning = false

    // Maps discovered mac -> BluetoothDevice for messaging
    private val peerDevices = mutableMapOf<String, android.bluetooth.BluetoothDevice>()

    // ── BLE Advertise Callback ─────────────────────────────────────────────────
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            println("BleTransportManager: Advertising started as '$displayName'")
        }
        override fun onStartFailure(errorCode: Int) {
            println("BleTransportManager: Advertise FAILED code=$errorCode")
        }
    }

    // ── BLE Scan Callback ──────────────────────────────────────────────────────
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val serviceData = result.scanRecord
                ?.getServiceData(ParcelUuid(BMTP_SERVICE_UUID))
            val peerName = serviceData
                ?.toString(Charsets.UTF_8)
                ?.trimEnd('\u0000')
                ?.takeIf { it.isNotBlank() }
                ?: result.scanRecord?.deviceName
                ?: result.device.address

            val mac = result.device.address
            peerDevices[mac] = result.device

            scope.launch {
                _discoveryFlow.emit(
                    BleDiscoveredNode(
                        id = mac,
                        name = peerName,
                        rssi = result.rssi,
                        mac = mac
                    )
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("BleTransportManager: Scan FAILED code=$errorCode")
        }
    }

    // ── GATT Server Callback (receive incoming messages) ─────────────────────
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: android.bluetooth.BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                peerDevices[device.address] = device
            }
        }

        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic.uuid == BMTP_MSG_CHAR_UUID && value != null) {
                val raw = String(value, Charsets.UTF_8)
                // Format: "senderId|message"
                val parts = raw.split("|", limit = 2)
                val senderId = parts.getOrElse(0) { device.address }
                val text = parts.getOrElse(1) { raw }
                scope.launch { _messageFlow.emit(BleMessage(senderId, text, false)) }
                if (responseNeeded) gattServer?.sendResponse(device, requestId, 0, 0, null)
            }
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun start(context: Context) {
        if (isRunning) return
        isRunning = true

        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        startGattServer(context)
        startAdvertising()
        startScanning()
    }

    fun stop() {
        isRunning = false
        val adapter = bluetoothManager?.adapter
        adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        gattServer?.close()
        gattServer = null
    }

    /** Send a text message to a peer by its MAC address over GATT write. */
    fun sendMessage(context: Context, peerMac: String, text: String) {
        val device = peerDevices[peerMac] ?: return
        scope.launch {
            try {
                val payload = "$nodeId|$text".toByteArray(Charsets.UTF_8)
                val gattClient = device.connectGatt(context, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) gatt.discoverServices()
                    }
                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        val service = gatt.getService(BMTP_SERVICE_UUID) ?: run { gatt.close(); return }
                        val char = service.getCharacteristic(BMTP_MSG_CHAR_UUID) ?: run { gatt.close(); return }
                        char.value = payload
                        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        gatt.writeCharacteristic(char)
                    }
                    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                        gatt.disconnect()
                        gatt.close()
                        scope.launch { _messageFlow.emit(BleMessage(nodeId, text, true)) }
                    }
                })
            } catch (e: Exception) {
                println("BleTransportManager: send failed: ${e.message}")
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun startGattServer(context: Context) {
        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
        val service = BluetoothGattService(BMTP_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val msgChar = BluetoothGattCharacteristic(
            BMTP_MSG_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(msgChar)
        gattServer?.addService(service)
    }

    private fun startAdvertising() {
        val adapter = bluetoothManager?.adapter ?: return
        val advertiser = adapter.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        // Encode displayName in service data so peers can read it during scan
        val nameBytes = displayName.toByteArray(Charsets.UTF_8).take(20).toByteArray()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BMTP_SERVICE_UUID))
            .addServiceData(ParcelUuid(BMTP_SERVICE_UUID), nameBytes)
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private fun startScanning() {
        val adapter = bluetoothManager?.adapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(500L)
            .build()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BMTP_SERVICE_UUID))
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
    }
}
