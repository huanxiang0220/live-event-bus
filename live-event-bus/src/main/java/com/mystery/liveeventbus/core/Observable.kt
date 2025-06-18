package com.mystery.liveeventbus.core

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

interface Observable<T> {

    /**
     * 进程内发送消息
     */
    fun post(value: T)

    /**
     * 注册一个Observer，生命周期感知，自动取消订阅
     */
    fun observe(owner: LifecycleOwner, observer: Observer<T>)

    /**
     * 注册一个Observer，生命周期感知，自动取消订阅，如果之前有消息发送，可以在注册时收到消息（消息同步）
     */
    fun observeSticky(owner: LifecycleOwner, observer: Observer<T>)

    //<editor-fold desc="慎用">
    /**
     * 注册一个Observer，需手动解除绑定
     */
    fun observeForever(observer: Observer<T>)

    /**
     * 注册一个Observer，需手动解除绑定
     * 如果之前有消息发送，可以在注册时收到消息（消息同步）
     *
     * @param observer 观察者
     */
    fun observeStickyForever(observer: Observer<T>)
    //</editor-fold>

    /**
     * 通过observeForever或observeStickyForever注册的，需要调用该方法取消订阅
     *
     * @param observer 观察者
     */
    fun removeObserver(observer: Observer<T>)
}