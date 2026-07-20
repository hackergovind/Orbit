package com.bmtp.app.protocol

import org.junit.Assert.*
import org.junit.Test

class ProtocolVersionTest {

    @Test
    fun `isSupported returns true for V1`() {
        assertTrue(ProtocolVersion.V1.isSupported())
    }

    @Test
    fun `isSupported returns false for UNKNOWN`() {
        assertFalse(ProtocolVersion.UNKNOWN.isSupported())
    }

    @Test
    fun `fromByte resolves correctly`() {
        assertEquals(ProtocolVersion.V1, ProtocolVersion.fromByte(1u))
        assertEquals(ProtocolVersion.UNKNOWN, ProtocolVersion.fromByte(255u))
        assertEquals(ProtocolVersion.UNKNOWN, ProtocolVersion.fromByte(2u)) // Future version not supported yet
    }
}
