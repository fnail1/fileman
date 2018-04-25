package ru.nailsoft.files.service;

import ru.nailsoft.files.model.FileItem;

public class ClipboardItem {
    public final FileItem file;
    public final boolean removeSource;

    public ClipboardItem(FileItem file, boolean removeSource) {
        this.file = file;
        this.removeSource = removeSource;
    }
}
