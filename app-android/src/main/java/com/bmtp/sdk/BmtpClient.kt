package com.bmtp.sdk

import com.bmtp.core.config.CoreConfig
import com.bmtp.core.logging.Logger
import com.bmtp.transport.Transport

class BmtpClient(
    val config: CoreConfig,
    val transport: Transport,
    val logger: Logger
) {
    fun start() {
        logger.log("BmtpClient started for ${config.nodeId}")
        transport.start()
    }
    
    fun stop() {
        logger.log("BmtpClient stopped")
        transport.stop()
    }
}
