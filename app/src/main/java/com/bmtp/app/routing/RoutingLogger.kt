package com.bmtp.app.routing

import com.bmtp.app.utils.LogUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standardized logging for the AODV Routing Engine.
 */
@Singleton
class RoutingLogger @Inject constructor() {
    private val subtag = "AODV_Routing"

    fun logRouteCreated(destId: String, nextHop: String, hops: UByte) {
        LogUtils.i(subtag, "Route Created -> Dest: $destId, NextHop: $nextHop, Hops: $hops")
    }

    fun logRouteRemoved(destId: String, reason: String) {
        LogUtils.i(subtag, "Route Removed -> Dest: $destId. Reason: $reason")
    }

    fun logDiscoveryStarted(destId: String, rreqId: UInt) {
        LogUtils.d(subtag, "Discovery Started -> Dest: $destId (RREQ_ID: $rreqId)")
    }

    fun logDiscoverySuccess(destId: String, hops: UByte) {
        LogUtils.d(subtag, "Discovery Success -> Dest: $destId at $hops hops")
    }

    fun logDiscoveryFailed(destId: String) {
        LogUtils.w(subtag, "Discovery Failed -> Dest: $destId")
    }
    
    fun logRreqForwarded(destId: String, hops: UByte) {
        LogUtils.v(subtag, "Forwarding RREQ for $destId, current hops: $hops")
    }
    
    fun logRrepSent(destId: String, targetHop: String) {
        LogUtils.v(subtag, "Sending RREP for $destId to $targetHop")
    }
    
    fun logRerrSent(destIds: List<String>, targetHop: String) {
        LogUtils.w(subtag, "Sending RERR for ${destIds.size} destinations to $targetHop")
    }

    fun logError(message: String, throwable: Throwable? = null) {
        LogUtils.e(subtag, message, throwable)
    }
}
