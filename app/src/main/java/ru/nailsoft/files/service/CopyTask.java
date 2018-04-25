package ru.nailsoft.files.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;

public class CopyTask implements Runnable {

    public static void listFiles(File src, File dst, ArrayList<CopyItem> out, boolean removeSource) {

        dst = new File(dst, src.getName());

        if (src.isDirectory()) {
            if (removeSource)
                out.add(new MoveDir(src, dst));
            else
                out.add(new CopyDir(src, dst));

            for (File file : src.listFiles()) {
                listFiles(file, dst, out, removeSource);
            }
        } else {
            if (removeSource)
                out.add(new MoveFile(src, dst));
            else
                out.add(new CopyFile(src, dst));
        }
    }


    private final File[] src;
    private final File dst;
    private final boolean removeSource;
    private boolean override;

    public CopyTask(File[] src, File dst, boolean removeSource, boolean overrideIfExist) {
        this.src = src;
        this.dst = dst;
        this.removeSource = removeSource;
        this.override = overrideIfExist;
    }

    @Override
    public void run() {
        ArrayList<CopyItem> queue = new ArrayList<>(src.length);
        for (File file : src) {
            listFiles(file, dst, queue, removeSource);
        }

        for (CopyItem item : queue) {
            item.prepare(this);
        }

        for (CopyItem item : queue) {
            try {
                item.doCopy(this);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FileOpException e) {
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

        public void doCopy(CopyTask task) throws IOException, FileOpException {
            if (dst.exists() && !task.override && !task.requestOverride(src))
                return;

            performCopy(task);
        }

        public void prepare(CopyTask task) {
        }

        public abstract void performCopy(CopyTask task) throws IOException, FileOpException;

        public void finalize(CopyTask task) {
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
        public void performCopy(CopyTask task) throws IOException {
            FileUtils.copyFile(src, dst);
        }
    }

    private static class CopyDir extends CopyItem {
        public CopyDir(File src, File dst) {
            super(src, dst);
        }

        @Override
        public void performCopy(CopyTask task) throws FileOpException {
            if (!dst.mkdir())
                throw new FileOpException(FileOpException.FileOp.MKDIR, dst);
        }
    }

    private static class MoveDir extends CopyDir {
        public MoveDir(File src, File dst) {
            super(src, dst);
        }

        @Override
        public void finalize(CopyTask task) {
            //noinspection ResultOfMethodCallIgnored
            src.delete();
        }
    }

    private static class MoveFile extends CopyItem {
        public MoveFile(File src, File dst) {
            super(src, dst);
        }

        @Override
        public void performCopy(CopyTask task) throws IOException, FileOpException {
            FileUtils.rename(src, dst);
        }
    }
}
