package com.antigravity.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bmtp.transport.BleTransportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class MeshNode(
    val id: String,   // MAC address
    val name: String, // Display name from BLE service data
    val rssi: Int,
    val hopCount: Int
)

data class MeshState(
    val isConnected: Boolean = false,
    val activeNodes: List<MeshNode> = emptyList(),
    val myNodeId: String = "",
    val myName: String = "Orbit User",
    val activeConnections: Int = 0,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val networkHealth: String = "Offline"
)

class MeshViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MeshState())
    val state: StateFlow<MeshState> = _state.asStateFlow()

    init {
        val myId = "Orbit-${UUID.randomUUID().toString().take(6)}"
        BleTransportManager.nodeId = myId
        _state.value = _state.value.copy(myNodeId = myId)

        // Collect real BLE discovered nodes
        viewModelScope.launch {
            BleTransportManager.discoveryFlow.collect { bleNode ->
                val currentNodes = _state.value.activeNodes.toMutableList()
                val existingIndex = currentNodes.indexOfFirst { it.id == bleNode.id }
                val meshNode = MeshNode(
                    id = bleNode.mac,
                    name = bleNode.name,
                    rssi = bleNode.rssi,
                    hopCount = 1
                )
                if (existingIndex == -1) {
                    currentNodes.add(meshNode)
                } else {
                    currentNodes[existingIndex] = meshNode
                }
                _state.value = _state.value.copy(
                    activeNodes = currentNodes,
                    activeConnections = currentNodes.size,
                    networkHealth = "Live — ${currentNodes.size} peer(s)"
                )
            }
        }
    }

    fun updateName(newName: String) {
        BleTransportManager.displayName = newName
        _state.value = _state.value.copy(myName = newName)
        // Restart advertising with new name if already running
        if (_state.value.isConnected) {
            BleTransportManager.stop()
            BleTransportManager.start(getApplication())
        }
    }

    fun startMesh() {
        BleTransportManager.start(getApplication())
        _state.value = _state.value.copy(isConnected = true, networkHealth = "Scanning BLE...")
    }

    fun stopMesh() {
        BleTransportManager.stop()
        _state.value = _state.value.copy(
            isConnected = false,
            activeNodes = emptyList(),
            networkHealth = "Offline"
        )
    }
}
