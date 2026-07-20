package com.bmtp.core.plugins

import com.bmtp.transport.Transport

/**
 * Defines the contract for extending the BMTP Core framework.
 * Plugins can register custom transports, cryptographic providers, or storage backends.
 */
interface BmtpPlugin {
    val pluginId: String
    val version: String

    /**
     * Called when the plugin is registered with the BMTP Engine.
     */
    fun onInitialize(registry: PluginRegistry)
    
    /**
     * Called when the engine shuts down.
     */
    fun onShutdown()
}

/**
 * Allows plugins to inject implementations into the core.
 */
interface PluginRegistry {
    /**
     * Registers a custom transport (e.g., AudioTransport, SatelliteBridge).
     */
    fun registerTransport(transport: Transport)

    /**
     * Registers a custom encryption provider (e.g., Post-Quantum Crypto).
     */
    fun registerCryptoProvider(providerId: String, provider: CryptoProvider)
}

/**
 * Abstract Cryptographic interface that can be overridden by plugins.
 */
interface CryptoProvider {
    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray
    fun sign(data: ByteArray, privateKey: ByteArray): ByteArray
    fun verify(data: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}
