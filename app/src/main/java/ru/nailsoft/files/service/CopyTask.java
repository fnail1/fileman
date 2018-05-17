package ru.nailsoft.files.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;

import static ru.nailsoft.files.App.copy;

public class CopyTask implements Runnable {


    private static void listFiles(ClipboardItem src, File srcFile, File dst, ArrayList<CopyItem> out) {

        dst = new File(dst, srcFile.getName());

        if (srcFile.isDirectory()) {
            out.add(new CopyDir(src, srcFile, dst));

            if (!src.removeSource) {
                for (File file : srcFile.listFiles()) {
                    listFiles(src, file, dst, out);
                }
            }
        } else {
            out.add(new CopyFile(src, srcFile, dst));
        }
    }

    private final Collection<ClipboardItem> src;
    private final TabData dst;
    private State state = State.NEW;
    private int progress = 0;
    private int count;
    private File currentFile;

    public CopyTask(Collection<ClipboardItem> src, TabData dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void run() {
        copy().onStart(this);
        state = State.ANALIZE;
        ArrayList<CopyItem> queue = new ArrayList<>(src.size());
        for (ClipboardItem file : src) {
            listFiles(file, file.file.file, dst.getPath(), queue);
            currentFile = file.file.file;
        }

        count = queue.size();
        for (CopyItem item : queue) {
            item.prepare(this);
            currentFile = item.src;
        }

        state = State.PROGRESS;
        for (CopyItem item : queue) {
            try {
                progress++;
                currentFile = item.src;
                item.doCopy(this);
            } catch (IOException | FileOpException e) {
                e.printStackTrace();
            }
        }

        state = State.FINALIZE;
        for (CopyItem item : queue) {
            item.finalize(this);
            currentFile = item.src;
        }
        for (ClipboardItem file : src) {
            dst.onPaste(file.file);
        }
        state = State.COMPLETE;
    }

    public String getCurrentFile() {
        File file = this.currentFile;
        if (file == null)
            return "";
        return file.getName();
    }

    private static abstract class CopyItem {
        protected final ClipboardItem clipboardItem;
        protected final File src;
        protected final File dst;

        private CopyItem(ClipboardItem clipboardItem, File src, File dst) {
            this.clipboardItem = clipboardItem;
            this.src = src;
            this.dst = dst;
        }

        void doCopy(CopyTask task) throws IOException, FileOpException {
            if (dst.exists() && !task.requestOverride(src))
                return;

            performCopy(task);
        }

        void prepare(CopyTask task) {
        }

        protected abstract void performCopy(CopyTask task) throws IOException, FileOpException;

        protected void finalize(CopyTask task) {
        }

    }

    private boolean requestOverride(File src) {
        return false;
    }

    private static class CopyFile extends CopyItem {

        private CopyFile(ClipboardItem clipboardItem, File src, File dst) {
            super(clipboardItem, src, dst);
        }

        @Override
        public void performCopy(CopyTask task) throws IOException, FileOpException {
            if (clipboardItem.removeSource)
                FileUtils.rename(src, dst);
            else
                FileUtils.copyFile(src, dst);
        }
    }

    private static class CopyDir extends CopyItem {
        private boolean complete;

        public CopyDir(ClipboardItem clipboardItem, File src, File dst) {
            super(clipboardItem, src, dst);
        }

        @Override
        public void performCopy(CopyTask task) throws FileOpException {
            complete = !clipboardItem.removeSource;
            if (clipboardItem.removeSource) {
                try {
                    FileUtils.rename(src, dst);
                    complete = true;
                } catch (FileOpException e) {
                    throw e;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (!dst.mkdir())
                    throw new FileOpException(FileOpException.FileOp.MKDIR, dst);
            }
        }

        @Override
        public void finalize(CopyTask task) {
            if (clipboardItem.removeSource && !complete) {
                FileUtils.deleteRecursive(src);
            }
            super.finalize(task);
        }
    }


    public State getState() {
        return state;
    }

    public int getProgress() {
        return progress;
    }

    public int getCount() {
        return count;
    }

    public enum State {
        NEW, ANALIZE, PROGRESS, FINALIZE, COMPLETE, FAIL
    }
}
