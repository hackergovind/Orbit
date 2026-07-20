package com.bmtp.production.simulator

import com.bmtp.core.config.CoreConfig
import com.bmtp.core.logging.StdOutLogger
import com.bmtp.sdk.BmtpClient
import com.bmtp.transport.MockTransport
import java.util.concurrent.atomic.AtomicLong

/**
 * Represents a single simulated device running the entire BMTP Core stack.
 *
 * Architecture Notes:
 * - Thread-safe: Each VirtualNode operates independently.
 * - Instantiates a real `BmtpClient` backed by a `MockTransport`.
 * - Exposes metrics for the benchmark runner.
 */
class VirtualNode(val id: String) {

    // Metrics for performance validation
    val packetsSent = AtomicLong(0)
    val packetsReceived = AtomicLong(0)
    val bytesTransferred = AtomicLong(0)
    
    // The actual protocol client
    private val config = CoreConfig(nodeId = id)
    val transport = MockTransport(nodeId = id)
    val client = BmtpClient(config, transport, StdOutLogger())

    /**
     * Boots the virtual node and joins the simulator mesh.
     */
    fun boot() {
        client.start()
    }

    /**
     * Shuts down the node cleanly.
     */
    fun shutdown() {
        client.stop()
    }

    /**
     * Queues a payload for delivery across the mesh.
     */
    fun send(destinationId: String, payload: ByteArray) {
        packetsSent.incrementAndGet()
        bytesTransferred.addAndGet(payload.size.toLong())
        client.sendMessage(destinationId, payload)
    }

    /**
     * Called by the MeshSimulator when a packet successfully arrives.
     * (Normally this is handled internally by the BmtpClient, but this hook allows metrics tracking).
     */
    internal fun recordReceive(sizeBytes: Int) {
        packetsReceived.incrementAndGet()
        bytesTransferred.addAndGet(sizeBytes.toLong())
    }
}
