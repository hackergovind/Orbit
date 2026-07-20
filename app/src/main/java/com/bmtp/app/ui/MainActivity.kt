package com.bmtp.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bmtp.app.service.BleMeshService
import com.bmtp.app.ui.discovery.DiscoveryScreen
import com.bmtp.app.ui.theme.BmtpTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the BMTP Application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start the background BLE Mesh Service
        BleMeshService.startService(this)

        setContent {
            BmtpTheme {
                DiscoveryScreen()
            }
        }
    }
}
