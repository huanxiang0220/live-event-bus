package com.mystery.liveeventbus.core;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.InternalLiveData;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.mystery.liveeventbus.logger.DefaultLogger;
import com.mystery.liveeventbus.logger.Logger;
import com.mystery.liveeventbus.logger.LoggerManager;
import com.mystery.liveeventbus.utils.ThreadUtils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BusCore {

    /**
     * 单例模式实现
     */
    private static class SingletonHolder {
        private static final BusCore DEFAULT_BUS = new BusCore();
    }

    public static BusCore get() {
        return SingletonHolder.DEFAULT_BUS;
    }

    /**
     * 存放LiveEvent
     */
    private final Map<String, LiveEvent<Object>> bus;
    private boolean lifecycleObserverAlwaysActive;
    private boolean autoClear;
    private final LoggerManager logger;
    private final Map<String, ObservableConfig> observableConfigs;

    public BusCore() {
        bus = new HashMap<>();
        observableConfigs = new HashMap<>();
        lifecycleObserverAlwaysActive = false;
        autoClear = false;
        logger = new LoggerManager(new DefaultLogger());
    }

    @NotNull
    public synchronized <T> Observable<T> with(@NotNull String key, @NotNull Class<T> type) {
        if (!bus.containsKey(key)) {
            bus.put(key, new LiveEvent<>(key));
        }
        return (Observable<T>) bus.get(key);
    }

    void setLogger(@NonNull Logger logger) {
        this.logger.setLogger(logger);
    }

    void enableLogger(boolean enable) {
        this.logger.setEnable(enable);
    }

    public ObservableConfig config(String key) {
        if (!observableConfigs.containsKey(key)) {
            observableConfigs.put(key, new ObservableConfig());
        }
        return observableConfigs.get(key);
    }

    private class LiveEvent<T> implements Observable<T> {

        private final String key;
        private final LifecycleLiveData<T> liveData;
        private final Map<Observer, ObserverWrapper<T>> observerMap = new HashMap<>();
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        public LiveEvent(String key) {
            this.key = key;
            this.liveData = new LifecycleLiveData<>(key);
        }

        //<editor-fold desc="非粘性">

        //<editor-fold desc="消息处理">

        /**
         * 进程内发送消息
         *
         * @param value :发送的消息
         */
        @Override
        public void post(T value) {
            if (ThreadUtils.isMainThread()) {
                postInternal(value);
            } else {
                mainHandler.post(new PostValueTask(value));
            }
        }

        private class PostValueTask implements Runnable {
            private final T newValue;

            public PostValueTask(@NonNull T newValue) {
                this.newValue = newValue;
            }

            @Override
            public void run() {
                postInternal(newValue);
            }
        }

        @MainThread
        private void postInternal(T value) {
            logger.log(Level.INFO, "post: " + value + " with key: " + key);
            liveData.setValue(value);
        }
        //</editor-fold>

        @Override
        public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
            if (ThreadUtils.isMainThread()) {
                observeInternal(owner, observer);
            } else {
                mainHandler.post(() -> observeInternal(owner, observer));
            }
        }

        @Override
        public void observeForever(@NonNull Observer<T> observer) {
            if (ThreadUtils.isMainThread()) {
                observeForeverInternal(observer);
            } else {
                mainHandler.post(() -> observeForeverInternal(observer));
            }
        }

        @MainThread
        private void observeInternal(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
            ObserverWrapper<T> observerWrapper = new ObserverWrapper<>(observer);
            observerWrapper.preventNextEvent = liveData.getVersion() > InternalLiveData.START_VERSION;
            liveData.observe(owner, observerWrapper);
            logger.log(Level.INFO, "observe observer: " + observerWrapper + "(" + observer + ")"
                    + " on owner: " + owner + " with key: " + key);
        }

        @MainThread
        private void observeForeverInternal(@NonNull Observer<T> observer) {
            ObserverWrapper<T> observerWrapper = new ObserverWrapper<>(observer);
            observerWrapper.preventNextEvent = liveData.getVersion() > InternalLiveData.START_VERSION;
            observerMap.put(observer, observerWrapper);
            liveData.observeForever(observerWrapper);
            logger.log(Level.INFO, "observe forever observer: " + observerWrapper + "(" + observer + ")"
                    + " with key: " + key);
        }
        //</editor-fold>

        @Override
        public void observeSticky(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
            if (ThreadUtils.isMainThread()) {
                observeStickyInternal(owner, observer);
            } else {
                mainHandler.post(() -> observeStickyInternal(owner, observer));
            }
        }

        @Override
        public void observeStickyForever(@NonNull Observer<T> observer) {
            if (ThreadUtils.isMainThread()) {
                observeStickyForeverInternal(observer);
            } else {
                mainHandler.post(() -> observeStickyForeverInternal(observer));
            }
        }

        @Override
        public void removeObserver(@NonNull Observer<T> observer) {
            if (ThreadUtils.isMainThread()) {
                removeObserverInternal(observer);
            } else {
                mainHandler.post(() -> removeObserverInternal(observer));
            }
        }

        @MainThread
        private void observeStickyInternal(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
            ObserverWrapper<T> observerWrapper = new ObserverWrapper<>(observer);
            liveData.observe(owner, observerWrapper);
            logger.log(Level.INFO, "observe sticky observer: " + observerWrapper + "(" + observer + ")"
                    + " on owner: " + owner + " with key: " + key);
        }

        @MainThread
        private void observeStickyForeverInternal(@NonNull Observer<T> observer) {
            ObserverWrapper<T> observerWrapper = new ObserverWrapper<>(observer);
            observerMap.put(observer, observerWrapper);
            liveData.observeForever(observerWrapper);
            logger.log(Level.INFO, "observe sticky forever observer: " + observerWrapper + "(" + observer + ")"
                    + " with key: " + key);
        }

        @MainThread
        private void removeObserverInternal(@NonNull Observer<T> observer) {
            Observer<T> realObserver;
            if (observerMap.containsKey(observer)) {
                realObserver = observerMap.remove(observer);
            } else {
                realObserver = observer;
            }
            liveData.removeObserver(realObserver);
        }
    }

    private class LifecycleLiveData<T> extends InternalLiveData<T> {

        private final String key;

        public LifecycleLiveData(String key) {
            this.key = key;
        }

        @Override
        protected Lifecycle.State observerActiveLevel() {
            return lifecycleObserverAlwaysActive() ? Lifecycle.State.CREATED : Lifecycle.State.STARTED;
        }

        @Override
        public void removeObserver(@NonNull Observer<? super T> observer) {
            super.removeObserver(observer);
            if (autoClear() && !hasObservers()) {
                BusCore.get().bus.remove(key);
            }
            logger.log(Level.INFO, "observer removed: " + observer);
        }

        private boolean lifecycleObserverAlwaysActive() {
            if (observableConfigs.containsKey(key)) {
                ObservableConfig config = observableConfigs.get(key);
                if (config.lifecycleObserverAlwaysActive != null) {
                    return config.lifecycleObserverAlwaysActive;
                }
            }
            return lifecycleObserverAlwaysActive;
        }

        private boolean autoClear() {
            if (observableConfigs.containsKey(key)) {
                ObservableConfig config = observableConfigs.get(key);
                if (config.autoClear != null) {
                    return config.autoClear;
                }
            }
            return autoClear;
        }
    }

    private class ObserverWrapper<T> implements Observer<T> {

        @NonNull
        private final Observer<T> observer;
        private boolean preventNextEvent = false;

        ObserverWrapper(@NonNull Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void onChanged(@Nullable T t) {
            if (preventNextEvent) {
                preventNextEvent = false;
                return;
            }
            logger.log(Level.INFO, "message received: " + t);
            try {
                observer.onChanged(t);
            } catch (ClassCastException e) {
                logger.log(Level.WARNING, "class cast error on message received: " + t, e);
            } catch (Exception e) {
                logger.log(Level.WARNING, "error on message received: " + t, e);
            }
        }
    }

}