package ru.nailsoft.files.model;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import ru.nailsoft.files.service.MyDocumentProvider;
import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.toolkit.io.FileUtils;

import static ru.nailsoft.files.diagnostics.DebugUtils.safeThrow;
import static ru.nailsoft.files.toolkit.collections.Query.query;

public class TabData {
    private final MainActivityData data;
    final Stack<AbsHistoryItem> history = new Stack<>();
    private List<FileItem> files = Collections.emptyList();
    public final HashSet<FileItem> selection = new HashSet<>();
    public String title;
    public List<FileItem> displayFiles = Collections.emptyList();
    private Order order = Order.NAME_ASC;

    public TabData(MainActivityData data) {
        this.data = data;
    }

    public AbsHistoryItem getPath() {
        return history.peek();
    }

    public void search(File root) {
        history.push(new SearchHistoryItem(root));
        data.onTabPathChanged(this);
    }

    public void navigate(File path) {
        history.push(new DirectoryHistoryItem(path));
        data.onTabPathChanged(this);
    }

//    public boolean navigateUp() {
//        File parent = getPath().path.getParentFile();
//        if (parent == null)
//            return false;
//
//        history.push(new HistoryItem(parent));
//        data.onTabPathChanged(this);
//        return true;
//    }

    public boolean navigateBack() {
        if (history.size() < 2)
            return false;
        history.pop();
        data.onTabPathChanged(this);
        return true;
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
        if (ThreadPool.isUiThread())
            safeThrow(new IllegalAccessException("Do not call me from main thread"));

        List<FileItem> filtered = getPath().applyFilter(files);

        Collections.sort(filtered, order.comarator());

        ThreadPool.UI.post(() -> {
            displayFiles = filtered;
            data.onTabDataChanged(this);
        });
    }

    @NonNull
    private static FileItem createDisplayFileItem(FileItem file, String name, int i, int length) {
        FileItem clone = file.clone();
        clone.title = clone.name;
        clone.subtitle = highlighFilteredName(name, i, length);
        return clone;
    }

    @NonNull
    private static SpannableStringBuilder highlighFilteredName(String name, int i, int length) {
        SpannableStringBuilder str = new SpannableStringBuilder(name);
        str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), i, i + length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return str;
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

    public void onRename(FileItem fileItem, File newFile) {
        int i = files.indexOf(fileItem);
        FileItem newFileItem = new FileItem(newFile);
        files.set(i, newFileItem);
        newFileItem.resolveDetails();
        onDataChanged();
    }

    public void onPaste(File dstFile) {
        FileItem newFileItem = new FileItem(dstFile);
        int i = files.indexOf(newFileItem);
        if (i < 0) {
            files.add(newFileItem);
            newFileItem.resolveDetails();
            onDataChanged();
        }
    }

    public void setFilter(String filter) {
        getPath().filter = filter;
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

    public void setOrder(Order order) {
        if (this.order == order)
            return;
        this.order = order;
        onDataChanged();
    }

    public Order getOrder() {
        return order;
    }

    public static abstract class AbsHistoryItem {
        protected String filter = "";

        Parcelable linearLayoutManagerSavedState;

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
    }

    public static class DirectoryHistoryItem extends AbsHistoryItem {
        public final File path;


        DirectoryHistoryItem(@NonNull File path) {
            this.path = path;
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
                List<FileItem> out = query(files).select(FileItem::new).toList();

                return out;
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
                return files;

            ArrayList<FileItem> filtered = new ArrayList<>(files.size());


            for (FileItem file : files) {
                String name = file.name;
                int i = name.toLowerCase().indexOf(filter);
                if (i >= 0) {
                    FileItem clone = createDisplayFileItem(file, name, i, filter.length());
                    filtered.add(clone);
                    continue;
                }


                if (file.file.isDirectory()) {
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
    }

    public class SearchHistoryItem extends AbsHistoryItem {

        public final File root;

        public SearchHistoryItem(File root) {
            this.root = root;
        }

        @Override
        public String title() {
            return null;
        }

        @Override
        public String subtitle() {
            return null;
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
    }


    public enum Order {
        NAME_ASC {
            @Override
            public Comparator<FileItem> comarator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return o1.order.compareTo(o2.order);
                };
            }
        },
        NAME_DESC {
            @Override
            public Comparator<FileItem> comarator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return o2.order.compareTo(o1.order);
                };
            }
        },
        SIZE_ASC {
            @Override
            public Comparator<FileItem> comarator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o2.size, o1.size);
                };
            }
        },
        SIZE_DESC {
            @Override
            public Comparator<FileItem> comarator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o1.size, o2.size);
                };
            }
        },
        MOFIFIED_ASC {
            @Override
            public Comparator<FileItem> comarator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o1.file.lastModified(), o2.file.lastModified());
                };
            }
        },
        MOFIFIED_DESC {
            @Override
            public Comparator<FileItem> comarator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o2.file.lastModified(), o1.file.lastModified());
                };
            }
        };

        public abstract Comparator<FileItem> comarator();

    }
}
