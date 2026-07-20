package com.bmtp.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

data class DiscoveredNode(val id: String, val name: String, val ip: String)
data class NetworkMessage(val senderId: String, val text: String, val isFromMe: Boolean)

object TransportManager {
    
    var nodeId: String = ""
    var displayName: String = "Orbit User"
    
    private val _discoveryFlow = MutableSharedFlow<DiscoveredNode>(extraBufferCapacity = 10)
    val discoveryFlow: SharedFlow<DiscoveredNode> = _discoveryFlow.asSharedFlow()

    private val _messageFlow = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 50)
    val messageFlow: SharedFlow<NetworkMessage> = _messageFlow.asSharedFlow()

    private var udpSocket: DatagramSocket? = null
    private var tcpServer: ServerSocket? = null
    private var isRunning = false

    private val activePeers = mutableMapOf<String, String>() // id -> ip

    fun start() {
        if (isRunning) return
        isRunning = true
        
        // Start UDP Listener for Discovery
        thread(isDaemon = true) {
            try {
                udpSocket = DatagramSocket(54321)
                val buffer = ByteArray(1024)
                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    val data = String(packet.data, 0, packet.length)
                    if (data.startsWith("ORBIT_DISCOVERY:")) {
                        val parts = data.removePrefix("ORBIT_DISCOVERY:").split("|")
                        if (parts.size == 2) {
                            val peerId = parts[0]
                            val peerName = parts[1]
                            val peerIp = packet.address.hostAddress
                            if (peerId != nodeId && peerIp != null) {
                                activePeers[peerId] = peerIp
                                _discoveryFlow.tryEmit(DiscoveredNode(peerId, peerName, peerIp))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore socket closed
            }
        }

        // Start UDP Broadcaster
        thread(isDaemon = true) {
            try {
                val broadcastSocket = DatagramSocket()
                broadcastSocket.broadcast = true
                while (isRunning) {
                    val msg = "ORBIT_DISCOVERY:$nodeId|$displayName".toByteArray()
                    val packet = DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), 54321)
                    broadcastSocket.send(packet)
                    Thread.sleep(3000) // Broadcast every 3 seconds
                }
            } catch (e: Exception) {
            }
        }

        // Start TCP Server for Messages
        thread(isDaemon = true) {
            try {
                tcpServer = ServerSocket(54322)
                while (isRunning) {
                    val client = tcpServer?.accept() ?: break
                    thread(isDaemon = true) { handleClient(client) }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            val reader = client.inputStream.bufferedReader()
            val msg = reader.readLine()
            if (msg != null && msg.startsWith("MSG:")) {
                val content = msg.removePrefix("MSG:")
                val parts = content.split("|", limit = 2)
                if (parts.size == 2) {
                    _messageFlow.tryEmit(NetworkMessage(parts[0], parts[1], false))
                }
            }
        } catch (e: Exception) {
        } finally {
            client.close()
        }
    }

    suspend fun sendMessage(targetId: String, text: String) {
        val targetIp = activePeers[targetId] ?: return
        withContext(Dispatchers.IO) {
            try {
                val socket = Socket(targetIp, 54322)
                val writer = socket.outputStream.bufferedWriter()
                writer.write("MSG:$nodeId|$text\n")
                writer.flush()
                socket.close()
                _messageFlow.emit(NetworkMessage(nodeId, text, true))
            } catch (e: Exception) {
                // Failed to send
            }
        }
    }

    fun stop() {
        isRunning = false
        udpSocket?.close()
        tcpServer?.close()
    }
}
