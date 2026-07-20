package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PacketValidatorTest {

    private lateinit validator: PacketValidatorImpl
    private lateinit cache: PacketCacheImpl
    private lateinit factory: PacketFactoryImpl

    @Before
    fun setup() {
        cache = PacketCacheImpl()
        validator = PacketValidatorImpl(cache)
        factory = PacketFactoryImpl()
    }

    @Test
    fun `valid packet passes validation`() {
        val packet = factory.createPing(ByteArray(16), ByteArray(16))
        validator.validate(packet) // Should not throw
    }

    @Test(expected = VersionMismatchException::class)
    fun `unsupported version throws exception`() {
        val original = factory.createPing(ByteArray(16), ByteArray(16))
        val header = original.header.copy(version = ProtocolVersion.UNKNOWN)
        validator.validate(original.copy(header = header))
    }

    @Test(expected = TtlExpiredException::class)
    fun `expired ttl throws exception`() {
        val original = factory.createPing(ByteArray(16), ByteArray(16))
        val header = original.header.copy(ttl = 0u)
        validator.validate(original.copy(header = header))
    }

    @Test(expected = DuplicatePacketException::class)
    fun `duplicate packet throws exception`() {
        val packet = factory.createPing(ByteArray(16), ByteArray(16))
        validator.validate(packet) // First time passes, caches ID
        validator.validate(packet) // Second time throws
    }
}
