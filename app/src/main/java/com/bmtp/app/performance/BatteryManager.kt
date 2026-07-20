package com.bmtp.app.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors device battery levels and charging state via Android System Broadcasts.
 * Updates the central MetricsCollector.
 */
@Singleton
class BmtpBatteryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: PerformanceConfig,
    private val metricsCollector: MetricsCollector,
    private val logger: PerformanceLogger
) {
    private var isRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL
                             
            val batteryPct = if (level >= 0 && scale > 0) {
                level.toFloat() / scale.toFloat()
            } else {
                1.0f // Fallback safely to full
            }
            
            val isCritical = batteryPct <= config.criticalBatteryThresholdPercent
            
            metricsCollector.updateBattery(batteryPct, isCharging, isCritical)
            
            // Log state transitions
            val currentMetrics = metricsCollector.metrics.value
            if (currentMetrics.isBatteryCritical != isCritical || currentMetrics.isCharging != isCharging) {
                logger.logBatteryModeChanged(isCritical, isCharging)
            }
        }
    }

    fun startMonitoring() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
            isRegistered = true
        }
    }

    fun stopMonitoring() {
        if (isRegistered) {
            context.unregisterReceiver(batteryReceiver)
            isRegistered = false
        }
    }
}
