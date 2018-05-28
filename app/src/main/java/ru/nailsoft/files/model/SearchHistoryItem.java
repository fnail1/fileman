package ru.nailsoft.files.model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import ru.nailsoft.files.toolkit.io.FileUtils;

import static ru.nailsoft.files.toolkit.collections.Query.query;

public class SearchHistoryItem extends AbsHistoryItem {

    private TabData data;
    public final File root;

    SearchHistoryItem(TabData data, File root) {
        this.data = data;
        this.root = root;
    }

    @Override
    public String title() {
        return root.getName();
    }

    @Override
    public String subtitle() {
        String subtitle = data.data.searchTabSubtitlePrefix;

        if (!TextUtils.isEmpty(filter))
            subtitle += filter;

        return subtitle;
    }

    @NonNull
    @Override
    public String id() {
        return "search:" + root.getAbsolutePath();
    }

    @Override
    public List<FileItem> readFiles() {
        return Collections.emptyList();
    }

    @Override
    public File anchor() {
        return root;
    }

    @Override
    public List<FileItem> applyFilter(List<FileItem> files) {
        List<File> found = null;
        String filter = this.filter.toLowerCase();

        if (!TextUtils.isEmpty(filter))
            found = FileUtils.searchRecursive(root, filter);

        if (found == null)
            return Collections.emptyList();

        List<FileItem> out = query(found).select(FileItem::new).toList();
        for (FileItem fileItem : out) {
            fileItem.resolveDetails();
            fileItem.title = highlighFilteredName(fileItem.name, fileItem.name.toLowerCase().indexOf(filter), filter.length());
        }

        return out;
    }

    @Override
    public void onNavigateTo(AbsHistoryItem next) {

    }


}
