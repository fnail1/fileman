package ru.nailsoft.files.model;

import android.os.Parcelable;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import static ru.nailsoft.files.toolkit.collections.Query.query;

public class TabData {
    private final MainActivityData data;
    public String title;
    public volatile List<FileItem> files = Collections.emptyList();
    final Stack<HistoryItem> history = new Stack<>();
    public final HashSet<FileItem> selection = new HashSet<>();

    public TabData(MainActivityData data) {
        this.data = data;
    }

    public File getPath() {
        return history.peek().path;
    }

    public void navigate(File path) {
        data.navigate(this, path);
    }

    public boolean navigateUp() {
        return data.navigateUp(this);
    }

    public boolean navigateBack() {
        return data.navigateBack(this);
    }

    public boolean toggleSelection(FileItem file) {
        boolean selected = selection.remove(file);
        if (!selected)
            selection.add(file);
        data.onSelectionChanged(this);
        return !selected;
    }

    public void onDataChanged() {
        data.onTabDataChanged(this);
    }

    public void onRename(FileItem fileItem, File newFile) {
        int i = files.indexOf(fileItem);
        FileItem newFileItem = new FileItem(newFile);
        files.set(i, newFileItem);
        newFileItem.resolveDetails();
        onDataChanged();
    }

    public Parcelable scrollState() {
        return history.peek().linearLayoutManagerSavedState;
    }

    public void saveScrollState(Parcelable linearLayoutManagerSavedState) {
        history.peek().linearLayoutManagerSavedState = linearLayoutManagerSavedState;
    }

    public static class HistoryItem {
        public final File path;
        public Parcelable linearLayoutManagerSavedState;

        public HistoryItem(File path) {
            this.path = path;
        }
    }
}
