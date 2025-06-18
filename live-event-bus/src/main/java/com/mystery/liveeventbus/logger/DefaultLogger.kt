package com.mystery.liveeventbus.logger

import android.util.Log
import java.util.logging.Level

class DefaultLogger : Logger {

    companion object {
        private const val TAG = "[LiveEventBus]"
    }

    override fun log(level: Level, msg: String) {
        log(level, msg, null)
    }

    override fun log(level: Level, msg: String, th: Throwable?) {
        if (level === Level.SEVERE) {
            Log.e(TAG, msg, th)
        } else if (level === Level.WARNING) {
            Log.w(TAG, msg, th)
        } else if (level === Level.INFO) {
            Log.i(TAG, msg, th)
        } else if (level === Level.CONFIG) {
            Log.d(TAG, msg, th)
        } else if (level !== Level.OFF) {
            Log.v(TAG, msg, th)
        }
    }

}