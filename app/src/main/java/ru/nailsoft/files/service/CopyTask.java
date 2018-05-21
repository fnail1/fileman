package ru.nailsoft.files.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;

import static ru.nailsoft.files.App.copy;

public class CopyTask extends AbsTask {


    private static void listFiles(ClipboardItem src, File srcFile, File dst, ArrayList<CopyItem> out) {
        if (srcFile.isDirectory()) {
            out.add(new CopyDir(src, srcFile, dst));

            if (!src.removeSource) {
                dst = new File(dst, srcFile.getName());
                for (File file : srcFile.listFiles()) {
                    listFiles(src, file, dst, out);
                }
            }
        } else if (src.extract) {
            out.add(new CopyArchive(src, dst));
        } else {
            out.add(new CopyFile(src, srcFile, dst));
        }
    }

    private final Collection<ClipboardItem> src;
    private final TabData dst;


    public CopyTask(Collection<ClipboardItem> src, TabData dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void run() {
        copy().onStart(this);
        setState(AbsTask.State.ANALIZE);
        ArrayList<CopyItem> queue = new ArrayList<>(src.size());
        for (ClipboardItem file : src) {
            listFiles(file, file.file.file, dst.getPath(), queue);
            setCurrentFile(file.file.file);
        }

        setCount(queue.size());
        for (CopyItem item : queue) {
            item.prepare(this);
            setCurrentFile(item.src);
        }

        setState(AbsTask.State.PROGRESS);
        for (CopyItem item : queue) {
            try {
                incrementProgress();
                setCurrentFile(item.src);
                item.doCopy(this);
            } catch (IOException | FileOpException e) {
                e.printStackTrace();
            }
        }

        setState(AbsTask.State.FINALIZE);
        for (CopyItem item : queue) {
            item.finalize(this);
            setCurrentFile(item.src);
        }

        setState(AbsTask.State.COMPLETE);
    }


    private boolean requestOverride(File src) {
        return false;
    }


    private static abstract class CopyItem {
        final ClipboardItem clipboardItem;
        protected final File src;
        final File dst;

        private CopyItem(ClipboardItem clipboardItem, File src, File dst) {
            this.clipboardItem = clipboardItem;
            this.src = src;
            this.dst = dst;
        }

        void doCopy(CopyTask task) throws IOException, FileOpException {
            if (!dst.exists())
                throw new FileNotFoundException("Destination directory not exists");

            performCopy(task);
        }

        void prepare(CopyTask task) {
        }

        abstract void performCopy(CopyTask task) throws IOException, FileOpException;

        void finalize(CopyTask task) {
        }

    }

    private static class CopyFile extends CopyItem {


        private CopyFile(ClipboardItem clipboardItem, File src, File dst) {
            super(clipboardItem, src, dst);
        }

        @Override
        void performCopy(CopyTask task) throws IOException, FileOpException {
            File out = new File(dst, src.getName());
            if (clipboardItem.removeSource)
                FileUtils.rename(src, out);
            else
                FileUtils.copyFile(src, out);

            if (clipboardItem.file.file == src) {
                task.dst.onPaste(out);
            }
        }

    }

    private static class CopyDir extends CopyItem {

        private boolean complete;

        CopyDir(ClipboardItem clipboardItem, File src, File dst) {
            super(clipboardItem, src, dst);
        }

        @Override
        void performCopy(CopyTask task) throws FileOpException {
            complete = !clipboardItem.removeSource;
            File out = new File(dst, src.getName());
            if (clipboardItem.removeSource) {
                try {
                    FileUtils.rename(src, out);
                    complete = true;
                    if (clipboardItem.file.file == src) {
                        task.dst.onPaste(out);
                    }
                } catch (FileOpException e) {
                    throw e;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (!out.mkdir())
                    throw new FileOpException(FileOpException.FileOp.MKDIR, out);

                if (clipboardItem.file.file == src) {
                    task.dst.onPaste(out);
                }
            }
        }

        @Override
        void finalize(CopyTask task) {
            if (clipboardItem.removeSource && !complete) {
                FileUtils.deleteRecursive(src);
            }
            super.finalize(task);
        }

    }


    private static class CopyArchive extends CopyItem {
        CopyArchive(ClipboardItem src, File dst) {
            super(src, src.file.file, dst);
        }

        @Override
        void performCopy(CopyTask task) throws IOException, FileOpException {
            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(src)))) {
                ZipEntry entry;
                int count;
                byte[] buffer = new byte[8192];
                while ((entry = zip.getNextEntry()) != null) {
                    File file = new File(dst, entry.getName());
                    File dir = entry.isDirectory() ? file : file.getParentFile();

                    if (dir.exists()) {
                        if (!dir.isDirectory())
                            throw new IOException("File already exists but it is not a directory");
                    } else if (!dir.mkdirs()) {
                        throw new FileOpException(FileOpException.FileOp.MKDIR, dir);
                    }

                    if (entry.isDirectory()) {
                        if (dir.getParentFile().equals(dst)) {
                            task.dst.onPaste(dir);
                        }
                        continue;
                    }

                    try (FileOutputStream out = new FileOutputStream(file)) {
                        while ((count = zip.read(buffer)) != -1)
                            out.write(buffer, 0, count);
                    }

                    long time = entry.getTime();
                    if (time > 0) {
                        //noinspection ResultOfMethodCallIgnored
                        file.setLastModified(time);
                    }

                    if (dir.equals(dst)) {
                        task.dst.onPaste(file);
                    }
                }
            }
        }

        @Override
        void finalize(CopyTask task) {
            super.finalize(task);
            if (clipboardItem.removeSource)
                src.delete();
        }
    }

}
