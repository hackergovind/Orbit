package com.bmtp.sdk

import com.bmtp.core.config.CoreConfig
import com.bmtp.core.events.BmtpEvent
import com.bmtp.core.events.EventBus
import com.bmtp.core.logging.PlatformLogger
import com.bmtp.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The Developer-facing API Facade for the BMTP Protocol.
 * Apps running on Android, Desktop, or CLI instantiate this client to interact with the mesh.
 *
 * It acts as the orchestration layer binding the physical Transport to the logical Core Engine.
 */
class BmtpClient(
    val config: CoreConfig,
    private val transport: Transport,
    private val logger: PlatformLogger
) {
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventBus = EventBus()

    // Core Engine Components (Will be fully implemented in subsequent phases)
    // private val routingEngine = RoutingEngine(eventBus, logger)
    // private val securityEngine = SecurityEngine(config, logger)

    /**
     * Exposes all system events to the application layer.
     */
    val events: Flow<BmtpEvent> = eventBus.events

    init {
        logger.i("BMTPClient", "Initializing BMTP SDK for Node: ${config.nodeId}")
    }

    /**
     * Starts the BMTP engine and the underlying transport.
     */
    fun start() {
        clientScope.launch {
            logger.i("BMTPClient", "Starting Transport: ${transport.transportId}")
            
            // Start listening to transport events and route them to the EventBus
            launch {
                transport.events().collect { event ->
                    logger.d("BMTPClient", "Transport Event: $event")
                    // Map TransportEvent to BmtpEvent and publish...
                }
            }

            // Start listening to raw packets and route them to the core packet parser
            launch {
                transport.receive().collect { (peerId, payload) ->
                    // pass payload to the protocol parser and security engine
                    logger.v("BMTPClient", "Received ${payload.size} bytes from $peerId")
                }
            }

            transport.start()
        }
    }

    /**
     * Stops the engine and shuts down the transport.
     */
    fun stop() {
        clientScope.launch {
            logger.i("BMTPClient", "Stopping Transport...")
            transport.stop()
        }
    }

    /**
     * Queues a message to be sent to a specific node over the mesh network.
     */
    fun sendMessage(destinationNodeId: String, payload: ByteArray) {
        clientScope.launch {
            logger.i("BMTPClient", "Queuing message for $destinationNodeId")
            
            // In reality, this delegates to the RoutingEngine to find the next hop,
            // packages it via the Protocol engine, encrypts it, and then calls transport.send()
            
            // Stubbed for this abstraction phase
            // val nextHop = routingEngine.getNextHop(destinationNodeId)
            // transport.send(nextHop, packagedPayload)
        }
    }
}
