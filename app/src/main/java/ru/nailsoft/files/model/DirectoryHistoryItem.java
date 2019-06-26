package ru.nailsoft.files.model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.nailsoft.files.toolkit.collections.Query.query;

public class DirectoryHistoryItem extends AbsHistoryItem {
    public final File path;


    DirectoryHistoryItem(@NonNull File path) {
        this.path = path;
    }

    private static String searchRecursive(File file, String filter) {

        for (String s : file.list()) {
            if (s.toLowerCase().contains(filter))
                return s;
        }

        for (File child : file.listFiles()) {
            if (child.isDirectory()) {
                String s = searchRecursive(child, filter);
                if (s != null)
                    return s;
            }
        }

        return null;
    }

    @Override
    public String title() {
        return path.getName();
    }

    @Override
    public String subtitle() {
        return path.getParent();
    }

    @Override
    @NonNull
    public String id() {
        return "dir:" + path.getAbsolutePath();
    }

    @Override
    public List<FileItem> readFiles() {
        File[] files = path.listFiles();
        if (files != null) {
            return query(files).select(FileItem::new).toList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public File anchor() {
        return path;
    }

    @Override
    public List<FileItem> applyFilter(List<FileItem> files) {
        String filter = this.filter.toLowerCase();

        if (TextUtils.isEmpty(filter))
            return new ArrayList<>(files);

        ArrayList<FileItem> filtered = new ArrayList<>(files.size());


        for (FileItem file : files) {
            String name = file.name;
            int i = name.toLowerCase().indexOf(filter);
            if (i >= 0) {
                FileItem clone = createDisplayFileItem(file, name, i, filter.length());
                filtered.add(clone);
                continue;
            }


            if (file.directory) {
                name = searchRecursive(file.file, filter);
                if (name != null) {
                    i = name.toLowerCase().indexOf(filter);
                    FileItem clone = createDisplayFileItem(file, name, i, filter.length());
                    filtered.add(clone);
                }
            }
        }

        return filtered;
    }

    @Override
    public void onNavigateTo(AbsHistoryItem next) {
        next.filter = filter;
    }

    @NonNull
    private static FileItem createDisplayFileItem(FileItem file, String name, int i, int length) {
        FileItem clone = file.clone();
        clone.title = clone.name;
        clone.subtitle = highlighFilteredName(name, i, length);
        return clone;
    }
}
