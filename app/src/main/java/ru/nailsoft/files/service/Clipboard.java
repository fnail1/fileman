package ru.nailsoft.files.service;

import java.util.Collection;
import java.util.LinkedHashMap;

import ru.nailsoft.files.model.FileItem;

public class Clipboard extends LinkedHashMap<FileItem, ClipboardItem> {
    public boolean toggleSelection(FileItem file, boolean removeSource) {
        boolean selected = containsKey(file);
        if (selected)
            remove(file);
        else
            put(file, new ClipboardItem(file, removeSource));

        return !selected;
    }


    public void addAll(Collection<FileItem> selection, boolean removeSource) {
        for (FileItem item : selection) {
            put(item, new ClipboardItem(item, removeSource));
        }
    }
}
