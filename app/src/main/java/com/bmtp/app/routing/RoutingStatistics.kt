package com.bmtp.app.routing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class holding AODV routing metrics.
 */
data class RoutingMetrics(
    val routesCreated: Long = 0,
    val routesDestroyed: Long = 0,
    val rreqSent: Long = 0,
    val rrepSent: Long = 0,
    val rerrSent: Long = 0,
    val discoverySuccess: Long = 0,
    val discoveryFailure: Long = 0,
    val repairCount: Long = 0,
    val activeRoutesCount: Int = 0
)

/**
 * Thread-safe singleton for tracking AODV routing statistics.
 */
@Singleton
class RoutingStatistics @Inject constructor() {
    private val _metrics = MutableStateFlow(RoutingMetrics())
    val metrics: StateFlow<RoutingMetrics> = _metrics.asStateFlow()

    fun recordRouteCreated() = _metrics.update { it.copy(routesCreated = it.routesCreated + 1) }
    fun recordRouteDestroyed() = _metrics.update { it.copy(routesDestroyed = it.routesDestroyed + 1) }
    fun recordRreqSent() = _metrics.update { it.copy(rreqSent = it.rreqSent + 1) }
    fun recordRrepSent() = _metrics.update { it.copy(rrepSent = it.rrepSent + 1) }
    fun recordRerrSent() = _metrics.update { it.copy(rerrSent = it.rerrSent + 1) }
    fun recordDiscoverySuccess() = _metrics.update { it.copy(discoverySuccess = it.discoverySuccess + 1) }
    fun recordDiscoveryFailure() = _metrics.update { it.copy(discoveryFailure = it.discoveryFailure + 1) }
    fun recordRepair() = _metrics.update { it.copy(repairCount = it.repairCount + 1) }
    
    fun updateActiveRoutesCount(count: Int) {
        _metrics.update { it.copy(activeRoutesCount = count) }
    }
}
