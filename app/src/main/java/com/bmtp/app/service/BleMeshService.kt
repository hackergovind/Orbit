package com.bmtp.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bmtp.app.bluetooth.BleConstants
import com.bmtp.app.repository.DeviceRepository
import com.bmtp.app.utils.DeviceIdGenerator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that runs the BLE Mesh operations in the background.
 */
@AndroidEntryPoint
class BleMeshService : LifecycleService() {

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var deviceIdGenerator: DeviceIdGenerator

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(BleConstants.NOTIFICATION_ID, createNotification(0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val deviceId = deviceIdGenerator.getOrCreateDeviceId()
        deviceRepository.startDiscovery(deviceId)
        
        observeDeviceCount()
        
        return START_STICKY
    }

    override fun onDestroy() {
        deviceRepository.stopDiscovery()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BleConstants.NOTIFICATION_CHANNEL_ID,
                "BMTP Mesh Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Runs the Bluetooth Mesh protocol in the background"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(peerCount: Int): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BleConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BMTP Mesh Active")
            .setContentText("Connected to $peerCount peers")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun observeDeviceCount() {
        lifecycleScope.launch {
            deviceRepository.nearbyDevices.collect { devices ->
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(BleConstants.NOTIFICATION_ID, createNotification(devices.size))
            }
        }
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, BleMeshService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BleMeshService::class.java)
            context.stopService(intent)
        }
    }
}
