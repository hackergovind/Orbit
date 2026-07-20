package com.bmtp.cli

import com.bmtp.core.config.CoreConfig
import com.bmtp.core.logging.StdOutLogger
import com.bmtp.sdk.BmtpClient
import com.bmtp.transport.MockTransport
import java.util.Scanner

/**
 * Entry point for the BMTP Command Line Interface.
 * Allows developers and users to interact with a BMTP Mesh directly from a terminal window
 * on macOS, Linux, or Windows without requiring Android.
 */
fun main(args: Array<String>) {
    val logger = StdOutLogger()
    
    val nodeId = if (args.isNotEmpty()) args[0] else "CLI-Node-${System.currentTimeMillis() % 1000}"
    val config = CoreConfig(nodeId = nodeId)
    
    // For now, we bind the CLI to the in-memory MockTransport.
    // In the future, we could pass args to bind to a TcpTransport or SerialPortTransport.
    val transport = MockTransport(nodeId)
    
    val client = BmtpClient(config, transport, logger)

    logger.i("CLI", "Welcome to BMTP Command Line Interface")
    logger.i("CLI", "Type 'help' for commands, or 'exit' to quit.")

    client.start()

    val scanner = Scanner(System.`in`)
    var running = true

    while (running) {
        print("bmtp@$nodeId > ")
        val input = scanner.nextLine().trim()
        
        when {
            input.isEmpty() -> continue
            input == "exit" || input == "quit" -> {
                logger.i("CLI", "Shutting down...")
                client.stop()
                running = false
            }
            input == "help" -> {
                println("""
                    Available commands:
                    ping <nodeId>     - Send a diagnostic ping to a node
                    send <nodeId> msg - Send a text message to a node
                    status            - Show current mesh routing table and active peers
                    exit              - Stop the client and exit
                """.trimIndent())
            }
            input.startsWith("send ") -> {
                val parts = input.split(" ", limit = 3)
                if (parts.size == 3) {
                    val dest = parts[1]
                    val msg = parts[2]
                    client.sendMessage(dest, msg.toByteArray())
                    println("Message queued for $dest")
                } else {
                    println("Usage: send <nodeId> <message>")
                }
            }
            input.startsWith("ping ") -> {
                val dest = input.removePrefix("ping ").trim()
                println("Pinging $dest... (Implementation pending)")
                // client.ping(dest)
            }
            input == "status" -> {
                println("Mesh Status: (Implementation pending)")
                // print routing table
            }
            else -> {
                println("Unknown command: $input")
            }
        }
    }
}
