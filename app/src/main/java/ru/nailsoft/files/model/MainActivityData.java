package ru.nailsoft.files.model;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.UiThread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ru.nailsoft.files.App;
import ru.nailsoft.files.R;
import ru.nailsoft.files.service.FileInfoCache;
import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.toolkit.events.ObservableEvent;

import static ru.nailsoft.files.App.app;
import static ru.nailsoft.files.diagnostics.Logger.trace;
import static ru.nailsoft.files.toolkit.collections.Query.query;

@SuppressWarnings("WeakerAccess")
public class MainActivityData {

    public final ObservableEvent<TabDataChangeEventHandler, MainActivityData, TabData> tabDataChanged = new ObservableEvent<TabDataChangeEventHandler, MainActivityData, TabData>(this) {
        @Override
        protected void notifyHandler(TabDataChangeEventHandler handler, MainActivityData sender, TabData args) {
            handler.onTabDataChanged(sender, args);
        }
    };

    public final ObservableEvent<TabsChangeEventHandler, MainActivityData, TabData> tabsChanged = new ObservableEvent<TabsChangeEventHandler, MainActivityData, TabData>(this) {
        @Override
        protected void notifyHandler(TabsChangeEventHandler handler, MainActivityData sender, TabData args) {
            handler.onTabsChanged(args);
        }
    };

    public final ObservableEvent<SelectionChangeEventHandler, MainActivityData, TabData> selectionChanged = new ObservableEvent<SelectionChangeEventHandler, MainActivityData, TabData>(this) {
        @Override
        protected void notifyHandler(SelectionChangeEventHandler handler, MainActivityData sender, TabData args) {
            handler.onSelectionChanged(args);
        }
    };

    public final String searchTabSubtitlePrefix;
    private final LinkedList<TabData> _tabs = new LinkedList<>();
    public final List<TabData> tabs = Collections.unmodifiableList(_tabs);
    private final FileInfoCache cache = new FileInfoCache();

    public MainActivityData(Context context) {
        newTab().navigate(Environment.getExternalStorageDirectory());
        searchTabSubtitlePrefix = "\uD83D\uDD0E ";
    }

    @SuppressWarnings("UnusedReturnValue")
    @UiThread
    public TabData newTab() {
        TabData tab = new TabData(this);
        _tabs.add(tab);
        return tab;
    }

    @UiThread
    public void onTabPathChanged(TabData tab) {
        updateTabNamesSync();
        tabsChanged.fire(tab);

        AbsHistoryItem path = tab.getPath();

        List<FileItem> cached = cache.get(path.id());
        boolean fromCache = cached != null;
        if (fromCache) {
            tab.setFiles(path, cached);
        }

        ThreadPool.QUICK_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
            List<FileItem> files = path.readFiles();
            ArrayList<FileItem> copy = new ArrayList<>(files);

            if (cached == null || cached.isEmpty()) {
                cache.put(path.id(), files);
                tab.setFiles(path, files);
            }

            long t0 = SystemClock.elapsedRealtime();
            for (int i = 0; i < copy.size(); i++) {
                FileItem file = copy.get(i);
                file.resolveDetails();
                if (!fromCache && ((i + 1) % 20) == 9) {
                    long t = SystemClock.elapsedRealtime();
                    if (t - t0 > 100) {
                        ThreadPool.UI.post(() -> onTabDataChanged(tab));
                        t0 = t;
                    }
                }
            }
            if (fromCache) {
                tab.setFiles(path, files);
            } else {
                onTabDataChanged(tab);
            }
        });
    }

    private void updateTabNamesSync() {
//        HashMap<String, ArrayList<TabData>> map = new HashMap<>();
//
//        for (TabData t : _tabs) {
//            ArrayList<TabData> l = map.get(t.getPath().id());
//            if (l == null) {
//                l = new ArrayList<>();
//                map.put(t.getPath().id(), l);
//            }
//            l.add(t);
//        }
//
//        for (ArrayList<TabData> l : map.values()) {
//            if (l.size() > 1) {
//                for (int i = 0; i < l.size(); i++) {
//                    TabData t = l.get(i);
//                    t.title = t.getPath().title() + " (" + (i + 1) + ")";
//                }
//            } else {
//                TabData t = l.get(0);
//                t.title = t.getPath().title();
//            }
//        }
    }

    @UiThread
    public void closeTab(TabData tab) {
        _tabs.remove(tab);
        onTabPathChanged(tab);
    }

    public void onTabDataChanged(TabData tabData) {
        tabDataChanged.fire(tabData);
    }

    public void onSelectionChanged(TabData tabData) {
        selectionChanged.fire(tabData);
    }

    public void onPermissionGranted() {
        for (TabData tab : tabs) {
            onTabPathChanged(tab);
        }
    }

    public interface TabDataChangeEventHandler {
        void onTabDataChanged(MainActivityData sender, TabData args);
    }

    public interface TabsChangeEventHandler {
        void onTabsChanged(TabData args);
    }

    public interface SelectionChangeEventHandler {
        void onSelectionChanged(TabData args);
    }

}
