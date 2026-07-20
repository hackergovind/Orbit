package com.bmtp.core.logging

interface Logger {
    fun log(msg: String)
}

class StdOutLogger : Logger {
    override fun log(msg: String) {
        println(msg)
    }
}
