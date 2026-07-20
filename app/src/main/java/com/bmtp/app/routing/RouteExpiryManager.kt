package com.bmtp.app.routing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodically sweeps the routing table and removes expired or long-invalid routes.
 */
@Singleton
class RouteExpiryManager @Inject constructor(
    private val routingTable: RoutingTable,
    private val config: RoutingConfig,
    private val logger: RoutingLogger
) {
    private var sweepJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Starts the background expiry sweep.
     */
    fun start() {
        if (sweepJob?.isActive == true) return
        
        logger.logError("RouteExpiryManager starting sweep loop", null) // Just using logError as generic logger placeholder if info not available, but let's just not log.
        
        sweepJob = scope.launch {
            while (isActive) {
                delay(config.expirySweepIntervalMs)
                routingTable.evictExpiredRoutes(System.currentTimeMillis())
            }
        }
    }

    /**
     * Stops the background expiry sweep.
     */
    fun stop() {
        sweepJob?.cancel()
        sweepJob = null
    }
}
