package com.antigravity.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmtp.transport.TransportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class MeshNode(
    val id: String,
    val name: String,
    val ip: String,
    val signalStrength: Int,
    val hopCount: Int
)

data class MeshState(
    val isConnected: Boolean = false,
    val activeNodes: List<MeshNode> = emptyList(),
    val myNodeId: String = "",
    val myName: String = "Orbit User",
    val activeConnections: Int = 0,
    val totalBytesSent: Long = 0,
    val totalBytesReceived: Long = 0,
    val networkHealth: String = "Good"
)

class MeshViewModel : ViewModel() {

    private val _state = MutableStateFlow(MeshState())
    val state: StateFlow<MeshState> = _state.asStateFlow()

    init {
        val myId = "Orbit-${UUID.randomUUID().toString().take(6)}"
        TransportManager.nodeId = myId
        _state.value = _state.value.copy(myNodeId = myId)

        viewModelScope.launch {
            TransportManager.discoveryFlow.collect { node ->
                val currentNodes = _state.value.activeNodes.toMutableList()
                val existingIndex = currentNodes.indexOfFirst { it.id == node.id }
                if (existingIndex == -1) {
                    currentNodes.add(MeshNode(node.id, node.name, node.ip, 100, 1))
                } else {
                    currentNodes[existingIndex] = MeshNode(node.id, node.name, node.ip, 100, 1)
                }
                _state.value = _state.value.copy(
                    activeNodes = currentNodes,
                    activeConnections = currentNodes.size,
                    networkHealth = "Live"
                )
            }
        }
    }

    fun updateName(newName: String) {
        TransportManager.displayName = newName
        _state.value = _state.value.copy(myName = newName)
    }

    fun startMesh() {
        TransportManager.start()
        _state.value = _state.value.copy(isConnected = true, networkHealth = "Scanning UDP...")
    }
    
    fun stopMesh() {
        TransportManager.stop()
        _state.value = _state.value.copy(isConnected = false, activeNodes = emptyList(), networkHealth = "Offline")
    }
}

