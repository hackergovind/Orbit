package com.bmtp.app.security_hardening

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces a hybrid rate-limiting strategy.
 * 1. A global token bucket to protect the device CPU from being overwhelmed by the entire mesh.
 * 2. A per-node token bucket to prevent a single malicious node from starving legitimate peers.
 */
@Singleton
class RateLimiter @Inject constructor(
    private val config: GovernanceConfig,
    private val metrics: SecurityMetrics,
    private val logger: AuditLogger
) {
    private val globalTokens = AtomicInteger(config.globalPacketBurstLimit)
    
    // Per-node buckets: Map<NodeId, Tokens>
    private val nodeTokens = ConcurrentHashMap<String, AtomicInteger>()
    
    private var lastRefillTime = System.currentTimeMillis()

    /**
     * Checks if a packet from [nodeId] should be processed.
     * Throws RateLimitExceededException if rejected.
     */
    @Throws(RateLimitExceededException::class)
    fun consumeToken(nodeId: String) {
        refillTokensIfNecessary()
        
        // 1. Check Global Limit
        if (globalTokens.decrementAndGet() < 0) {
            globalTokens.incrementAndGet() // Revert
            metrics.incrementRateLimitEvents()
            logger.logIncident("GLOBAL_RATE_LIMIT", nodeId, "Global queue full")
            throw RateLimitExceededException("Global rate limit exceeded")
        }
        
        // 2. Check Per-Node Limit
        val nodeBucket = nodeTokens.getOrPut(nodeId) { AtomicInteger(config.perNodePacketBurstLimit) }
        
        if (nodeBucket.decrementAndGet() < 0) {
            nodeBucket.incrementAndGet() // Revert
            // Revert the global token we mistakenly consumed
            globalTokens.incrementAndGet() 
            metrics.incrementRateLimitEvents()
            logger.logIncident("NODE_RATE_LIMIT", nodeId, "Node exceeded its burst limit")
            throw RateLimitExceededException("Node rate limit exceeded for $nodeId")
        }
    }

    /**
     * Refills buckets every second.
     */
    private fun refillTokensIfNecessary() {
        val now = System.currentTimeMillis()
        if (now - lastRefillTime > 1000L) {
            synchronized(this) {
                if (now - lastRefillTime > 1000L) {
                    globalTokens.set(config.globalPacketBurstLimit)
                    nodeTokens.forEach { (_, bucket) -> 
                        bucket.set(config.perNodePacketBurstLimit) 
                    }
                    lastRefillTime = now
                }
            }
        }
    }
}
