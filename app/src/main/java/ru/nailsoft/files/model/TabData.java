package ru.nailsoft.files.model;

import android.os.Parcelable;
import android.support.annotation.UiThread;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import ru.nailsoft.files.service.MyDocumentProvider;
import ru.nailsoft.files.toolkit.ThreadPool;

import static ru.nailsoft.files.diagnostics.DebugUtils.safeThrow;
import static ru.nailsoft.files.toolkit.collections.Query.query;

public class TabData {
    final MainActivityData data;
    private final Stack<AbsHistoryItem> history = new Stack<>();
    private List<FileItem> files = Collections.emptyList();
    public final HashSet<FileItem> selection = new HashSet<>();
    //    public String title;
    public List<FileItem> displayFiles = Collections.emptyList();
    private Order order = Order.NAME_ASC;

    public TabData(MainActivityData data) {
        this.data = data;
    }

    public AbsHistoryItem getPath() {
        return history.peek();
    }

    public void search(File root) {
        SearchHistoryItem item = new SearchHistoryItem(this, root);
        history.push(item);
        data.onTabPathChanged(this);
    }

    public void navigate(File path) {
        DirectoryHistoryItem item = new DirectoryHistoryItem(path);
        if (history.size() > 0)
            getPath().onNavigateTo(item);
        history.push(item);
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
        if (ThreadPool.isUiThread()) {
            ThreadPool.QUICK_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(this::onDataChanged);
            return;
        }

        List<FileItem> filtered = getPath().applyFilter(files);

        Collections.sort(filtered, order.comparator());

        ThreadPool.UI.post(() -> {
            displayFiles = filtered;
            data.onTabDataChanged(this);
        });
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

    public List<FileItem> getFiles(@SuppressWarnings("unused") MyDocumentProvider.TabDataFriend friend) {
        return files;
    }

    public void setFiles(AbsHistoryItem path, List<FileItem> files) {
        if (!ThreadPool.isUiThread()) {
            ThreadPool.UI.post(() -> setFiles(path, files));
        } else {
            if (path.id().equals(getPath().id())) {
                this.files = files;
            }
        }
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

    public enum Order {
        NAME_ASC {
            @Override
            public Comparator<FileItem> comparator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return o1.order.compareTo(o2.order);
                };
            }
        },
        NAME_DESC {
            @Override
            public Comparator<FileItem> comparator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return o2.order.compareTo(o1.order);
                };
            }
        },
        SIZE_ASC {
            @Override
            public Comparator<FileItem> comparator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o2.size, o1.size);
                };
            }
        },
        SIZE_DESC {
            @Override
            public Comparator<FileItem> comparator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o1.size, o2.size);
                };
            }
        },
        MOFIFIED_ASC {
            @Override
            public Comparator<FileItem> comparator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o1.lastModified(), o2.lastModified());
                };
            }
        },
        MOFIFIED_DESC {
            @Override
            public Comparator<FileItem> comparator() {
                return (o1, o2) -> {
                    if (o1.directory != o2.directory)
                        return o1.directory ? -1 : 1;

                    return Long.compare(o2.lastModified(), o1.lastModified());
                };
            }
        };

        public abstract Comparator<FileItem> comparator();

    }
}
