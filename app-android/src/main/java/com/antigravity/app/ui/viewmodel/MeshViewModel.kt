package com.antigravity.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmtp.core.config.CoreConfig
import com.bmtp.core.logging.StdOutLogger
import com.bmtp.sdk.BmtpClient
import com.bmtp.transport.MockTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class MeshState(
    val isConnected: Boolean = false,
    val activeNodes: List<String> = emptyList(),
    val myNodeId: String = ""
)

class MeshViewModel : ViewModel() {

    private val _state = MutableStateFlow(MeshState())
    val state: StateFlow<MeshState> = _state.asStateFlow()

    private var client: BmtpClient? = null

    init {
        // Initialize the Antigravity SDK
        val myId = "User-${UUID.randomUUID().toString().take(4)}"
        val config = CoreConfig(nodeId = myId)
        val transport = MockTransport(nodeId = myId) // In a real app, this would be BluetoothLeTransport
        
        client = BmtpClient(config, transport, StdOutLogger())
        
        _state.value = _state.value.copy(myNodeId = myId)
    }

    fun startMesh() {
        viewModelScope.launch {
            client?.start()
            _state.value = _state.value.copy(isConnected = true)
            
            // Mock discovering some peers over time
            kotlinx.coroutines.delay(2000)
            _state.value = _state.value.copy(activeNodes = listOf("Node-Alpha", "Node-Bravo"))
        }
    }
    
    fun stopMesh() {
        client?.stop()
        _state.value = _state.value.copy(isConnected = false, activeNodes = emptyList())
    }
}
