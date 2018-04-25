package ru.nailsoft.files.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.toolkit.events.ObservableEvent;

import static ru.nailsoft.files.App.app;
import static ru.nailsoft.files.App.prefs;
import static ru.nailsoft.files.diagnostics.Logger.trace;


public class AppStateObserver {
    private final ObservableEvent<DateChangedEventHandler, AppStateObserver, Void> dateChangedEvent = new ObservableEvent<DateChangedEventHandler, AppStateObserver, Void>(this) {
        @Override
        protected void notifyHandler(DateChangedEventHandler handler, AppStateObserver sender, Void args) {
            handler.onDateTimeChanged();
        }
    };

    private final ObservableEvent<AppStateEventHandler, AppStateObserver, Void> stateEvent = new ObservableEvent<AppStateEventHandler, AppStateObserver, Void>(this) {
        @Override
        protected void notifyHandler(AppStateEventHandler handler, AppStateObserver sender, Void args) {
            handler.onAppStateChanged();
        }
    };

    private final ObservableEvent<LowMemoryEventHandler, AppStateObserver, Void> lowMemoryEvent = new ObservableEvent<LowMemoryEventHandler, AppStateObserver, Void>(this) {
        @Override
        protected void notifyHandler(LowMemoryEventHandler handler, AppStateObserver sender, Void args) {
            handler.onLowMemory();
        }
    };

    private Activity topActivity;
    private Activity closedActivity;
    public boolean initialized;
    private boolean serverTimeOffsetDirty;
    private Runnable onBackgroundTask = this::onBackground;

    public void init() {
        initialized = true;
    }

    public void addDateChangedEvent(DateChangedEventHandler handler) {
        dateChangedEvent.add(handler);
    }

    public void removeDateChangedEvent(DateChangedEventHandler handler) {
        dateChangedEvent.remove(handler);
    }

    public Activity getTopActivity() {
        return topActivity;
    }

    public void setTopActivity(@NonNull Activity topActivity) {
        trace("%s", topActivity);
        if (closedActivity != null) {
            closedActivity = null;
            ThreadPool.UI.removeCallbacks(onBackgroundTask);
        }

        if (this.topActivity != topActivity) {
            this.topActivity = topActivity;
            onStateChanged();
        }
    }

    public void resetTopActivity(@NonNull Activity activity) {
        trace("%s", activity);
        if (topActivity == activity) {
            closedActivity = activity;
            ThreadPool.UI.postDelayed(onBackgroundTask, 3000);
        }
    }

    private void onBackground() {
        boolean actual = topActivity == closedActivity;
        trace(Boolean.toString(actual));
        if (actual) {
            topActivity = null;
            closedActivity = null;
            onStateChanged();
        }
    }

    public boolean isForeground() {
        return topActivity != null;
    }

    public void addStateEventHandler(AppStateEventHandler handler) {
        stateEvent.add(handler);
    }

    public void removeStateEventHandler(AppStateEventHandler handler) {
        stateEvent.remove(handler);
    }

    public void onStateChanged() {
        stateEvent.fire(null);
    }

    public void addLowMemoryEventHandler(LowMemoryEventHandler handler) {
        lowMemoryEvent.add(handler);
    }

    public void removeLowMemoryEventHandler(LowMemoryEventHandler handler) {
        lowMemoryEvent.remove(handler);
    }

    public void onLowMemory() {
        // java.lang.IllegalArgumentException: You must call this method on the main thread
//        Glide.get(app()).clearMemory();
        lowMemoryEvent.fire(null);
    }

    public interface AppStateEventHandler {
        void onAppStateChanged();
    }

    public interface LowMemoryEventHandler {
        void onLowMemory();
    }

    public interface DateChangedEventHandler {
        void onDateTimeChanged();
    }
}
