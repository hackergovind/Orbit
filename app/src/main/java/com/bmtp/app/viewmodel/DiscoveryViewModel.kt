package com.bmtp.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmtp.app.bluetooth.BlePermissionManager
import com.bmtp.app.repository.DeviceRepository
import com.bmtp.app.utils.DeviceIdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the state of the Discovery screen.
 */
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val permissionManager: BlePermissionManager,
    private val deviceIdGenerator: DeviceIdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Loading)
    /** The current UI state of the discovery process. */
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    /** Transient error messages to be displayed in a snackbar. */
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        checkInitialState()
    }

    /**
     * Evaluates current permissions and Bluetooth state to determine next action.
     */
    private fun checkInitialState() {
        when {
            !permissionManager.isBluetoothSupported() -> {
                _uiState.value = DiscoveryUiState.BluetoothUnsupported
            }
            !permissionManager.hasRequiredPermissions() -> {
                _uiState.value = DiscoveryUiState.PermissionsRequired(permissionManager.getRequiredPermissions())
            }
            !permissionManager.isBluetoothEnabled() -> {
                _uiState.value = DiscoveryUiState.BluetoothDisabled
            }
            else -> {
                startDiscovery()
            }
        }
    }

    /**
     * Handle result from requesting permissions.
     */
    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            if (permissionManager.isBluetoothEnabled()) {
                startDiscovery()
            } else {
                _uiState.value = DiscoveryUiState.BluetoothDisabled
            }
        } else {
            _uiState.value = DiscoveryUiState.PermissionsRequired(permissionManager.getRequiredPermissions())
        }
    }

    /**
     * Handle when Bluetooth is enabled by the user.
     */
    fun onBluetoothEnabled() {
        checkInitialState()
    }

    /**
     * Refreshes the state, useful for lifecycle resumes.
     */
    fun refreshState() {
        checkInitialState()
    }

    private fun startDiscovery() {
        val deviceId = deviceIdGenerator.getOrCreateDeviceId()
        val result = deviceRepository.startDiscovery(deviceId)
        
        result.onSuccess {
            observeDevices()
        }.onFailure {
            _uiState.value = DiscoveryUiState.Error(it.message ?: "Unknown error occurred starting discovery.")
        }
    }

    /**
     * Stop the discovery process.
     */
    fun stopDiscovery() {
        deviceRepository.stopDiscovery()
        // State will update via the observeDevices flows.
    }

    /**
     * Toggles the scanning state.
     */
    fun toggleScanning() {
        if (deviceRepository.isScanning.value) {
            stopDiscovery()
        } else {
            startDiscovery()
        }
    }

    private fun observeDevices() {
        viewModelScope.launch {
            combine(
                deviceRepository.nearbyDevices,
                deviceRepository.isScanning,
                deviceRepository.isAdvertising
            ) { devices, scanning, advertising ->
                DiscoveryUiState.Discovering(
                    devices = devices,
                    isScanning = scanning,
                    isAdvertising = advertising
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        viewModelScope.launch {
            deviceRepository.errors.collect { errorMsg ->
                _snackbarEvents.emit(errorMsg)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
