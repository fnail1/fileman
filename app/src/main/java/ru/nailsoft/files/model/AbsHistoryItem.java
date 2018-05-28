package ru.nailsoft.files.model;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import java.io.File;
import java.util.List;

public abstract class AbsHistoryItem {
    protected String filter = "";

    Parcelable linearLayoutManagerSavedState;

    @NonNull
    static SpannableStringBuilder highlighFilteredName(String name, int i, int length) {
        SpannableStringBuilder str = new SpannableStringBuilder(name);
        str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, i + length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return str;
    }

    public abstract String title();

    public abstract String subtitle();

    @NonNull
    public abstract String id();

    public boolean same(AbsHistoryItem other) {
        return id().equals(other.id());
    }

    public abstract List<FileItem> readFiles();

    public abstract File anchor();

    public abstract List<FileItem> applyFilter(List<FileItem> files);

    public String getFilter() {
        return filter;
    }

    public abstract void onNavigateTo(AbsHistoryItem next);
}
