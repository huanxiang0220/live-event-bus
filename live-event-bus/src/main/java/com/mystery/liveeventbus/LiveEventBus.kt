package com.mystery.liveeventbus

import com.mystery.liveeventbus.core.BusCore
import com.mystery.liveeventbus.core.Observable

object LiveEventBus {

    @JvmStatic
    @JvmOverloads
    fun <T> get(
        key: String,
        type: Class<T>,
        alwaysActive: Boolean = false,/*观察者始终处于活动状态*/
    ): Observable<T> {
        val observable = BusCore.get().with(key, type)
        if (alwaysActive) {
            BusCore.get().config(key)
                .lifecycleObserverAlwaysActive(true)
                .autoClear(true)
        }
        return observable
    }

    @JvmStatic
    fun <T> get(
        key: String,
    ): Observable<T> {
        return get(key, Any::class.java) as Observable<T>
    }

    @JvmStatic
    @JvmOverloads
    fun <T> get(
        eventType: Class<T>,
        alwaysActive: Boolean = false,/*观察者始终处于活动状态*/
    ): Observable<T> {
        return get(eventType.name, eventType, alwaysActive)
    }

}