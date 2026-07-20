package com.bmtp.transport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.delay

@OptIn(ExperimentalCoroutinesApi::class)
class TransportTests {

    @Test
    fun `MockTransport - packets are routed between peers`() = runTest {
        val nodeA = MockTransport("NodeA")
        val nodeB = MockTransport("NodeB")

        nodeA.start()
        nodeB.start()

        val testPayload = "Hello Mesh".toByteArray()

        // Launch a receiver for NodeB
        var receivedFrom = ""
        var receivedData = ByteArray(0)
        
        val job = launch {
            val (from, data) = nodeB.receive().first()
            receivedFrom = from
            receivedData = data
        }

        // NodeA sends to NodeB
        val sent = nodeA.send("NodeB", testPayload)
        
        assertEquals(true, sent)
        
        // Wait for coroutine to process
        job.join()
        
        assertEquals("NodeA", receivedFrom)
        assertArrayEquals(testPayload, receivedData)

        nodeA.stop()
        nodeB.stop()
    }
}
