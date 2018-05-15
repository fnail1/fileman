package ru.nailsoft.files;

import android.app.Application;

import ru.nailsoft.files.model.MainActivityData;
import ru.nailsoft.files.service.AppStateObserver;
import ru.nailsoft.files.service.Clipboard;
import ru.nailsoft.files.service.CopyService;
import ru.nailsoft.files.ui.ScreenMetrics;
import ru.nailsoft.files.utils.photomanager.IconsManager;

public class App extends Application {

    private static App instance;
    private Preferences preferences;
    private AppStateObserver stateObserver;
    private Clipboard clipboard;
    private MainActivityData data;
    private ScreenMetrics screenMetrics;
    private IconsManager icons;
    private CopyService copyService;

    public static App app() {
        return instance;
    }

    public static Preferences prefs() {
        return instance.preferences;
    }

    public static AppStateObserver appState() {
        return instance.stateObserver;
    }

    public static Clipboard clipboard() {
        return instance.clipboard;
    }

    public static MainActivityData data() {
        return instance.data;
    }

    public static ScreenMetrics screenMetrics() {
        return instance.screenMetrics;
    }

    public static IconsManager icons() {
        return instance.icons;
    }

    public static CopyService copy() {
        return instance.copyService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = new Preferences(this);
        clipboard = new Clipboard();
        data = new MainActivityData();
        stateObserver = new AppStateObserver();
        screenMetrics = new ScreenMetrics(this);
        icons = new IconsManager(this, stateObserver, screenMetrics);
        copyService = new CopyService();
        instance = this;
    }
}
