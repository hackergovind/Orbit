package com.bmtp.app.viewmodel

import com.bmtp.app.models.DiscoveredDevice

/**
 * Represents all possible UI states for the Discovery screen.
 */
sealed interface DiscoveryUiState {
    /** Loading state while initializing. */
    data object Loading : DiscoveryUiState

    /** State when permissions are required. */
    data class PermissionsRequired(val permissions: List<String>) : DiscoveryUiState

    /** State when Bluetooth is disabled. */
    data object BluetoothDisabled : DiscoveryUiState

    /** State when Bluetooth is not supported on the device. */
    data object BluetoothUnsupported : DiscoveryUiState

    /** State when actively discovering devices. */
    data class Discovering(
        val devices: List<DiscoveredDevice>,
        val isScanning: Boolean,
        val isAdvertising: Boolean
    ) : DiscoveryUiState

    /** State representing an error. */
    data class Error(val message: String) : DiscoveryUiState
}
