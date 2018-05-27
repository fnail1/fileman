package ru.nailsoft.files.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import ru.nailsoft.files.model.FileItem;

public class FileInfoCache {
    private final HashMap<String, List<FileItem>> cache = new HashMap<>();
    private final String[] index = new String[20];
    private int pointer = 0;


    public void put(String key, List<FileItem> value) {
        index[pointer++ % index.length] = key;
        if (pointer > index.length) {
            cache.remove(index[pointer % index.length]);
        }
        cache.put(key, value);
    }

    public List<FileItem> get(String key) {
        return cache.get(key);
    }
}
