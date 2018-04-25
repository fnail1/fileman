package ru.nailsoft.files.model;

import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.UiThread;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.toolkit.events.ObservableEvent;

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

    private final LinkedList<TabData> _tabs = new LinkedList<>();
    public final List<TabData> tabs = Collections.unmodifiableList(_tabs);

    public MainActivityData() {
        newTab(Environment.getExternalStorageDirectory());
    }

    @UiThread
    public TabData newTab(File path) {
        TabData tab = new TabData(this);
        tab.history.push(new TabData.HistoryItem(path));
        _tabs.add(0, tab);
        onTabPathChanged(tab);
        return tab;
    }

    @UiThread
    public void onTabPathChanged(TabData tab) {
        updateTabNamesSync();
        tabsChanged.fire(tab);

        ThreadPool.QUICK_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(() -> {
            readFiles(tab);

            ThreadPool.UI.post(() -> onTabDataChanged(tab));

            List<FileItem> files = tab.files;
            long t0 = SystemClock.elapsedRealtime();
            for (int i = 0; i < files.size(); i++) {
                FileItem file = files.get(i);
                file.resolveDetails();
                if (((i + 1) % 10) == 9) {
                    long t = SystemClock.elapsedRealtime();
                    if (t - t0 > 100) {
                        ThreadPool.UI.post(() -> onTabDataChanged(tab));
                        t0 = t;
                    }
                }
            }
        });
    }

    private void readFiles(TabData tab) {
        File[] files = tab.getPath().listFiles();
        if (files != null) {
            tab.files = query(files).select(FileItem::new).toList();
            Collections.sort(tab.files, (o1, o2) -> {
                if (o1.directory == o2.directory)
                    return o1.order.compareTo(o2.order);
                if (o1.directory)
                    return -1;
                return 1;
            });
        } else {
            tab.files = Collections.emptyList();
        }
    }

    private void updateTabNamesSync() {
        HashMap<File, ArrayList<TabData>> map = new HashMap<>();

        for (TabData t : _tabs) {
            ArrayList<TabData> l = map.get(t.getPath());
            if (l == null) {
                l = new ArrayList<>();
                map.put(t.getPath(), l);
            }
            l.add(t);
        }

        for (ArrayList<TabData> l : map.values()) {
            if (l.size() > 1) {
                for (int i = 0; i < l.size(); i++) {
                    TabData t = l.get(i);
                    t.title = t.getPath().getName() + " (" + (i + 1) + ")";
                }
            } else {
                TabData t = l.get(0);
                t.title = t.getPath().getName();
            }
        }
    }

    @UiThread
    public void navigate(TabData tab, File path) {
        tab.history.push(new TabData.HistoryItem(path));
        onTabPathChanged(tab);
    }

    @UiThread
    public boolean navigateUp(TabData tab) {
        File parent = tab.getPath().getParentFile();
        if (parent == null)
            return false;

        navigate(tab, parent);
        return true;
    }

    @UiThread
    public boolean navigateBack(TabData tab) {
        if (tab.history.size() < 2)
            return false;
        tab.history.pop();
        onTabPathChanged(tab);
        return true;
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
