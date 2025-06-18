package com.mystery.liveeventbus.logger

import java.util.logging.Level

interface Logger {

    fun log(level: Level, msg: String)

    fun log(level: Level, msg: String, th: Throwable?)
}