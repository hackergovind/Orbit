package com.bmtp.app.group

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background Service that manages the lifecycle of the Group Communication Layer.
 */
@Singleton
class GroupService @Inject constructor(
    private val config: GroupConfig,
    private val groupPersistence: GroupPersistence,
    private val membershipSync: MembershipSync,
    private val presenceManager: PresenceManager,
    private val logger: GroupLogger
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var serviceJob: Job? = null
    
    fun startService() {
        if (serviceJob?.isActive == true) return
        
        serviceJob = scope.launch {
            try {
                logger.logError("Starting GroupService...", null)
                
                // 1. Restore state from disk
                groupPersistence.loadAllState()
                
                // 2. Start Membership Eventual Consistency Sync Loop
                membershipSync.startSyncLoop()
                
                // 3. Start Presence Eviction Loop
                startPresenceEvictionLoop()
                
            } catch (e: Exception) {
                logger.logError("Failed to start GroupService", e)
            }
        }
    }

    fun stopService() {
        scope.launch {
            membershipSync.stopSyncLoop()
            groupPersistence.saveAllState()
            serviceJob?.cancel()
            serviceJob = null
            logger.logError("Stopped GroupService", null)
        }
    }
    
    private fun startPresenceEvictionLoop() = scope.launch {
        while (isActive) {
            delay(config.presenceHeartbeatIntervalMs)
            presenceManager.evictStalePresence()
        }
    }
}
