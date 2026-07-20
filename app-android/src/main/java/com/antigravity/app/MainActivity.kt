package com.antigravity.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.antigravity.app.ui.navigation.AppNavigation
import com.antigravity.app.ui.theme.AntigravityTheme

class MainActivity : ComponentActivity() {

    // All permissions needed for BLE on Android 6-13+
    private val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ (API 31+)
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        // Android 6-11
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // Launcher to enable Bluetooth if off
    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, "Bluetooth must be enabled for Orbit Mesh to work.", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher to request all BLE permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "BLE permissions are required. Go to Settings → Apps → Orbit Mesh → Permissions and grant Bluetooth + Location.",
                Toast.LENGTH_LONG
            ).show()
        }
        // Even if partial, try to enable BT
        ensureBluetoothEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: request BLE permissions
        requestBlePermissions()

        setContent {
            AntigravityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestBlePermissions() {
        val notGranted = blePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        } else {
            ensureBluetoothEnabled()
        }
    }

    private fun ensureBluetoothEnabled() {
        val btManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null) {
            Toast.makeText(this, "This device does not support Bluetooth.", Toast.LENGTH_LONG).show()
            return
        }
        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            try {
                enableBtLauncher.launch(enableBtIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Please enable Bluetooth manually.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
