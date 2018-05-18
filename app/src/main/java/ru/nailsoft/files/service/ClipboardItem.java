package ru.nailsoft.files.service;

import ru.nailsoft.files.model.FileItem;

public class ClipboardItem {
    public final FileItem file;
    public final boolean removeSource;
    public final boolean extract;

    public ClipboardItem(FileItem file, boolean removeSource, boolean extract) {
        this.file = file;
        this.removeSource = removeSource;
        this.extract = extract;
    }
}
