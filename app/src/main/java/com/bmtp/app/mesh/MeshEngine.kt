package com.bmtp.app.mesh

import com.bmtp.app.protocol.PacketParser
import com.bmtp.app.utils.DeviceIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface that provides a stream of incoming raw bytes from the BLE layer.
 */
interface MeshReceiver {
    /**
     * Called by the BLE layer when raw bytes arrive.
     * @param sourceDeviceId The ID of the neighbor that sent the bytes.
     * @param data The raw byte payload.
     */
    fun onRawPacketReceived(sourceDeviceId: String, data: ByteArray)
}

/**
 * The central orchestrator for the BMTP Mesh Forwarding Engine.
 * Responsible for packet ingestion, filtering, and dispatching to delivery or relay components.
 */
@Singleton
class MeshEngine @Inject constructor(
    private val packetParser: PacketParser,
    private val packetFilter: PacketFilter,
    private val duplicateManager: DuplicateManager,
    private val deliveryManager: DeliveryManager,
    private val relayEngine: RelayEngine,
    private val forwardQueue: ForwardQueue,
    private val forwardingEngine: ForwardingEngine,
    private val neighborManager: NeighborManager,
    private val logger: MeshLogger,
    private val stats: MeshStatistics,
    private val config: MeshConfig,
    deviceIdGenerator: DeviceIdGenerator
) : MeshReceiver {

    private val myDeviceIdBytes = hexStringToByteArray(deviceIdGenerator.getOrCreateDeviceId())
    
    private val _meshState = MutableStateFlow<MeshState>(MeshState.Idle)
    val meshState: StateFlow<MeshState> = _meshState.asStateFlow()

    private var retryJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default)

    init {
        _meshState.value = MeshState.Discovering
        startQueueRetryLoop()
    }

    override fun onRawPacketReceived(sourceDeviceId: String, data: ByteArray) {
        engineScope.launch {
            processRawPacket(sourceDeviceId, data)
        }
    }

    private suspend fun processRawPacket(sourceDeviceId: String, data: ByteArray) {
        stats.incrementReceived()
        
        try {
            // 1. Parse
            val packet = packetParser.parse(data)
            logger.logReceived(packet.header.packetIdAsString(), sourceDeviceId)

            // 2. Filter (validates structure, TTL, hop limits)
            val filterResult = packetFilter.evaluate(packet)
            if (filterResult is FilterResult.Reject) {
                logger.logDropped(packet.header.packetIdAsString(), filterResult.reason)
                stats.incrementDropped()
                return
            }

            // 3. Duplicate Detection (Specific for Mesh routing)
            if (duplicateManager.isDuplicate(packet.header.packetIdAsString())) {
                logger.logDropped(packet.header.packetIdAsString(), "Duplicate packet")
                return
            }

            // 4. Determine Destination
            val isForMe = packet.isForNode(myDeviceIdBytes)
            val isBroadcast = packet.isBroadcast

            if (isForMe || isBroadcast) {
                deliveryManager.deliverLocally(packet)
            }

            if (!isForMe || isBroadcast) {
                // Must be relayed to others (controlled flooding)
                _meshState.value = MeshState.Relaying
                
                // Add tiny random delay to prevent broadcast storms / collisions
                delay(config.relayDelayMs)
                
                relayEngine.relayPacket(packet, sourceDeviceId)
                _meshState.value = MeshState.Connected
            }

        } catch (e: Exception) {
            logger.logError("Failed to process incoming raw packet", e)
            stats.incrementDropped()
        }
    }

    /**
     * Originates a new packet from this node and injects it into the mesh.
     * 
     * @param packet The fully formed packet from PacketFactory.
     */
    fun sendPacket(packet: Packet) {
        engineScope.launch {
            stats.incrementSent()
            
            // Log it in our duplicate cache so we don't echo our own packets if they route back
            duplicateManager.isDuplicate(packet.header.packetIdAsString())
            
            _meshState.value = MeshState.Relaying
            forwardingEngine.forward(packet, sourceDeviceId = null)
            _meshState.value = MeshState.Connected
        }
    }

    /**
     * Background loop that periodically attempts to forward packets stuck in the ForwardQueue.
     */
    private fun startQueueRetryLoop() {
        retryJob = engineScope.launch {
            while (isActive) {
                delay(config.retryDelayMs)
                
                if (neighborManager.hasNeighbors() && !forwardQueue.isEmpty()) {
                    val packetsToRetry = forwardQueue.dequeueAllValid()
                    for (packet in packetsToRetry) {
                        forwardingEngine.forward(packet, sourceDeviceId = null)
                    }
                }
                
                if (!neighborManager.hasNeighbors() && !forwardQueue.isEmpty()) {
                    _meshState.value = MeshState.Waiting
                }
            }
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
