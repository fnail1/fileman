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

    public void addAll(Collection<FileItem> selection, boolean removeSource) {
        addAll(selection, removeSource, false);
    }

    public void addAll(Collection<FileItem> selection, boolean removeSource, boolean extract) {
        for (FileItem item : selection) {
            ClipboardItem clipboardItem = new ClipboardItem(item, removeSource, extract);
            data.put(item, clipboardItem);
        }

        onChanged(new ClipboardEventArgs());
    }

    protected void onChanged(ClipboardEventArgs args) {
        changedEvent.fire(args);
    }

    public void clear() {
        data.clear();
        onChanged(new ClipboardEventArgs());
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Collection<ClipboardItem> values() {
        return new ArrayList<>(data.values());
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
        public ClipboardEventArgs() {

        }
//        public final Collection<FileItem> added;
//        public final Collection<FileItem> removed;
    }
}
