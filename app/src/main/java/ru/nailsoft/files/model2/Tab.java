package ru.nailsoft.files.model2;

import java.util.Stack;

import ru.nailsoft.files.toolkit.events.ObservableEvent;

public class Tab {
    private final Stack<AbsTabData> history = new Stack<>();

    public final ObservableEvent<TabPathChangedEventHandler, Tab, AbsTabData> tabPathChangedEvent = new ObservableEvent<TabPathChangedEventHandler, Tab, AbsTabData>(this) {
        @Override
        protected void notifyHandler(TabPathChangedEventHandler handler, Tab sender, AbsTabData args) {
            handler.onTabPathChanged(sender, args);
        }
    };

    public Tab() {

    }

    public void search(AbsDirectoryItem path) {
        navigateTo(new SearchTabData(this, path));
    }

    public void navigate(AbsDirectoryItem path) {
        navigateTo(new DirectoryTabData(this, path));
    }

    public boolean navigateBack() {
        if (history.size() == 1)
            return false;
        history.pop();
        onTabPathChanged(history.peek());
        return true;
    }

    private void navigateTo(AbsTabData item) {
        history.push(item);
        onTabPathChanged(item);
    }

    protected void onTabPathChanged(AbsTabData args) {
        tabPathChangedEvent.fire(args);
    }

    public interface TabPathChangedEventHandler {
        void onTabPathChanged(Tab sender, AbsTabData args);
    }
}
