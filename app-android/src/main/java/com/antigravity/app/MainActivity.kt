package com.antigravity.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.app.ui.screens.ChatScreen
import com.antigravity.app.ui.screens.DiscoveryScreen
import com.antigravity.app.ui.theme.AntigravityTheme
import com.antigravity.app.ui.theme.DeepBlack
import com.antigravity.app.ui.viewmodel.ChatViewModel
import com.antigravity.app.ui.viewmodel.MeshViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // In a real app, request permissions here before proceeding:
        // requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, ...), 100)

        setContent {
            AntigravityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepBlack
                ) {
                    AntigravityApp()
                }
            }
        }
    }
}

enum class Screen {
    Discovery,
    Chat
}

@Composable
fun AntigravityApp() {
    var currentScreen by remember { mutableStateOf(Screen.Discovery) }
    var targetNodeId by remember { mutableStateOf<String?>(null) }
    
    val meshViewModel: MeshViewModel = viewModel()
    
    when (currentScreen) {
        Screen.Discovery -> {
            DiscoveryScreen(
                viewModel = meshViewModel,
                onNodeSelected = { nodeId ->
                    targetNodeId = nodeId
                    currentScreen = Screen.Chat
                }
            )
        }
        Screen.Chat -> {
            targetNodeId?.let { nodeId ->
                // In Compose, you'd usually use a ViewModel factory with Navigation Compose
                // For simplicity here, we instantiate manually
                val chatViewModel = remember(nodeId) { ChatViewModel(nodeId) }
                
                ChatScreen(
                    targetNodeId = nodeId,
                    viewModel = chatViewModel,
                    onBack = { currentScreen = Screen.Discovery }
                )
            }
        }
    }
}
