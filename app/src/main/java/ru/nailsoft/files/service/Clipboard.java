package ru.nailsoft.files.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.toolkit.events.ObservableEvent;

public class Clipboard {
    private final LinkedHashMap<FileItem, ClipboardItem> data = new LinkedHashMap<>();
    public final ObservableEvent<ClipboardEventHandler, Clipboard, ClipboardEventArgs> changedEvent = new ObservableEvent<ClipboardEventHandler, Clipboard, ClipboardEventArgs>(this) {
        @Override
        protected void notifyHandler(ClipboardEventHandler handler, Clipboard sender, ClipboardEventArgs args) {
            handler.onClipboardChanged(args);
        }
    };

    public boolean toggleSelection(FileItem file, boolean removeSource) {
        boolean selected = data.containsKey(file);
        if (selected) {
            ClipboardItem item = data.remove(file);
            onChanged(new ClipboardEventArgs(null, Collections.singleton(item)));
        } else {
            ClipboardItem clipboardItem = new ClipboardItem(file, removeSource);
            data.put(file, clipboardItem);
            onChanged(new ClipboardEventArgs(Collections.singleton(clipboardItem), null));
        }


        return !selected;
    }


    public void addAll(Collection<FileItem> selection, boolean removeSource) {
        ArrayList<ClipboardItem> items = new ArrayList<>(selection.size());
        for (FileItem item : selection) {
            ClipboardItem clipboardItem = new ClipboardItem(item, removeSource);
            if (data.put(item, clipboardItem) == null)
                items.add(clipboardItem);
        }

        onChanged(new ClipboardEventArgs(items, null));
    }

    protected void onChanged(ClipboardEventArgs args) {
        changedEvent.fire(args);
    }

    public void clear() {
        ArrayList<ClipboardItem> items = new ArrayList<>(data.values());
        data.clear();
        onChanged(new ClipboardEventArgs(null, items));
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Collection<ClipboardItem> values() {
        return data.values();
    }

    public int size() {
        return data.size();
    }

    public ClipboardItem get(FileItem file) {
        return data.get(file);
    }

    public interface ClipboardEventHandler {

        void onClipboardChanged(ClipboardEventArgs args);
    }

    public class ClipboardEventArgs {
        public ClipboardEventArgs(Collection<ClipboardItem> added, Collection<ClipboardItem> removed) {

        }
//        public final Collection<FileItem> added;
//        public final Collection<FileItem> removed;
    }
}
