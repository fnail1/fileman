package ru.nailsoft.files.model;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import ru.nailsoft.files.service.MyDocumentProvider;
import ru.nailsoft.files.toolkit.ThreadPool;

public class TabData {
    private final MainActivityData data;
    final Stack<HistoryItem> history = new Stack<>();
    private List<FileItem> files = Collections.emptyList();
    public final HashSet<FileItem> selection = new HashSet<>();
    public String title;
    public List<FileItem> displayFiles = Collections.emptyList();
    private String filter = "";

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

    public void selectAll() {
        selectAll(files);
    }

    @SuppressWarnings("WeakerAccess")
    public void selectAll(Collection<FileItem> file) {
        selection.addAll(file);
        data.onSelectionChanged(this);
    }

    public void onDataChanged() {
        List<FileItem> filtered;

        if (TextUtils.isEmpty(filter)) {
            filtered = files;
        } else {
            filtered = new ArrayList<>(files.size());

            List<FileItem> files = this.files;
            for (FileItem file : files) {
                String name = file.name;
                int i = name.toLowerCase(Locale.getDefault()).indexOf(filter);
                if (i >= 0) {
                    FileItem clone = createDisplayFileItem(file, name, i);
                    filtered.add(clone);
                    continue;
                }


                if (file.file.isDirectory()) {
                    name = searchRecursive(file.file, filter);
                    if (name != null) {
                        i = name.toLowerCase(Locale.getDefault()).indexOf(filter);
                        FileItem clone = createDisplayFileItem(file, name, i);
                        filtered.add(clone);
                    }
                }
            }
        }

        ThreadPool.UI.post(() -> {
            displayFiles = filtered;
            data.onTabDataChanged(this);
        });
    }

    @NonNull
    private FileItem createDisplayFileItem(FileItem file, String name, int i) {
        FileItem clone = file.clone();
        SpannableStringBuilder str = new SpannableStringBuilder(name);
        str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i + 1, i + filter.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        clone.subtitle = str;
        return clone;
    }

    private String searchRecursive(File file, String filter) {

        for (String s : file.list()) {
            if (s.toLowerCase(Locale.getDefault()).contains(filter))
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

    public void onRename(FileItem fileItem, File newFile) {
        int i = files.indexOf(fileItem);
        FileItem newFileItem = new FileItem(newFile);
        files.set(i, newFileItem);
        newFileItem.resolveDetails();
        onDataChanged();
    }

    public void onPaste(FileItem srcFileItem) {
        File dstFile = new File(getPath(), srcFileItem.file.getName());
        FileItem newFileItem = new FileItem(dstFile);
        int i = files.indexOf(newFileItem);
        if (i < 0) {
            files.add(newFileItem);
            newFileItem.resolveDetails();
            onDataChanged();
        }
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.toLowerCase(Locale.getDefault());
        onDataChanged();
    }

    public void onDelete(FileItem fileItem) {
        files.remove(fileItem);
        onDataChanged();
    }

    public Parcelable scrollState() {
        return history.peek().linearLayoutManagerSavedState;
    }

    public void saveScrollState(Parcelable linearLayoutManagerSavedState) {
        history.peek().linearLayoutManagerSavedState = linearLayoutManagerSavedState;
    }

    public List<FileItem> getFiles(MyDocumentProvider.TabDataFriend friend) {
        return files;
    }

    @UiThread
    public void setFiles(List<FileItem> files) {
        this.files = files;
        onDataChanged();
    }

    public static class HistoryItem {
        public final File path;
        Parcelable linearLayoutManagerSavedState;

        HistoryItem(File path) {
            this.path = path;
        }
    }
}
