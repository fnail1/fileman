package ru.nailsoft.files.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;

public class DeleteTask implements Runnable {

    public static void listFiles(File src, File dst, ArrayList<CopyItem> out) {

        dst = new File(dst, src.getName());

        if (src.isDirectory()) {
            out.add(new CopyDir(src, dst));

            for (File file : src.listFiles()) {
                listFiles(file, dst, out);
            }
        } else {
            out.add(new CopyFile(src, dst));
        }
    }

    private final Collection<ClipboardItem> src;
    private final File dst;

    public DeleteTask(Collection<ClipboardItem> src, File dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public void run() {
        ArrayList<CopyItem> queue = new ArrayList<>(src.size());
        for (ClipboardItem file : src) {
            listFiles(file.file.file, dst, queue);
        }

        for (CopyItem item : queue) {
            item.prepare(this);
        }

        for (CopyItem item : queue) {
            try {
                item.doCopy(this);
            } catch (IOException | FileOpException e) {
                e.printStackTrace();
            }
        }

        for (CopyItem item : queue) {
            item.finalize(this);
        }
    }

    private static abstract class CopyItem {
        public final File src;
        public final File dst;

        private CopyItem(File src, File dst) {
            this.src = src;
            this.dst = dst;
        }

        public void doCopy(DeleteTask task) throws IOException, FileOpException {
            if (dst.exists() && !task.requestOverride(src))
                return;

            performCopy(task);
        }

        public void prepare(DeleteTask task) {
        }

        public abstract void performCopy(DeleteTask task) throws IOException, FileOpException;

        public void finalize(DeleteTask task) {
        }

    }

    private boolean requestOverride(File src) {
        return false;
    }

    private static class CopyFile extends CopyItem {
        private CopyFile(File src, File dst) {
            super(src, dst);
        }

        @Override
        public void performCopy(DeleteTask task) throws IOException {
            FileUtils.copyFile(src, dst);
        }
    }

    private static class CopyDir extends CopyItem {
        public CopyDir(File src, File dst) {
            super(src, dst);
        }

        @Override
        public void performCopy(DeleteTask task) throws FileOpException {
            if (!dst.mkdir())
                throw new FileOpException(FileOpException.FileOp.MKDIR, dst);
        }
    }

}
