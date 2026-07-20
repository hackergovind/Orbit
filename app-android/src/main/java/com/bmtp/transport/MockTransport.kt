package com.bmtp.transport

interface Transport {
    fun start()
    fun stop()
}

class MockTransport(val nodeId: String) : Transport {
    override fun start() {
        println("MockTransport started for $nodeId")
    }
    
    override fun stop() {
        println("MockTransport stopped for $nodeId")
    }
}
