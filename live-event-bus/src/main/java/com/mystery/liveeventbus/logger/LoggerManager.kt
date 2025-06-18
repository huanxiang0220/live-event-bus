package com.mystery.liveeventbus.logger

import java.util.logging.Level

class LoggerManager(var logger: Logger) : Logger {

    var enable: Boolean = true

    override fun log(level: Level, msg: String) {
        if (enable) {
            logger.log(level, msg)
        }
    }

    override fun log(level: Level, msg: String, th: Throwable?) {
        if (enable) {
            logger.log(level, msg, th)
        }
    }
}