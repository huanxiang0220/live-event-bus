package com.mystery.liveeventbus.utils

import android.os.Looper

internal object ThreadUtils {

    @JvmStatic
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}