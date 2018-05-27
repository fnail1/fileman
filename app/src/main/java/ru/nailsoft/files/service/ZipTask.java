package ru.nailsoft.files.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.model.TabData;

import static ru.nailsoft.files.App.copy;

public class ZipTask extends AbsTask {


    private static void listFiles(FileItem src, File srcFile, ArrayList<TaskItem> out) {
        out.add(new TaskItem(src.file.getParentFile(), srcFile));

        if (srcFile.isDirectory()) {
            for (File file : srcFile.listFiles()) {
                listFiles(src, file, out);
            }
        }
    }

    private final Collection<FileItem> src;
    private final TabData dst;
    private final String name;


    public ZipTask(Collection<FileItem> src, TabData dst, String name) {
        this.src = src;
        this.dst = dst;
        this.name = name;
    }

    @Override
    public void run() {
        copy().onStart(this);
        setState(State.ANALIZE);
        ArrayList<TaskItem> queue = new ArrayList<>(src.size());
        for (FileItem file : src) {
            listFiles(file, file.file, queue);
            setCurrentFile(file.file);
        }

        setCount(queue.size());
        setState(State.PROGRESS);

        File zip = new File(((TabData.DirectoryHistoryItem) dst.getPath()).path, name);

        try {
            try (FileOutputStream dest = new FileOutputStream(zip)) {

                try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {
                    byte buffer[] = new byte[16 * 1024];

                    for (TaskItem item : queue) {
                        incrementProgress();
                        setCurrentFile(item.path);

                        String fileName = item.path.getAbsolutePath().substring(item.root.getAbsolutePath().length());

                        if (item.path.isDirectory()) {
                            ZipEntry entry = new ZipEntry(fileName + '/');
                            out.putNextEntry(entry);
                        } else {

                            FileInputStream fi = new FileInputStream(item.path);
                            try (BufferedInputStream origin = new BufferedInputStream(fi, 16 * 1024)) {
                                ZipEntry entry = new ZipEntry(fileName);
                                out.putNextEntry(entry);
                                int count;
                                while ((count = origin.read(buffer, 0, buffer.length)) != -1) {
                                    out.write(buffer, 0, count);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dst.onPaste(zip);
        }

        setState(State.FINALIZE);
        setState(State.COMPLETE);
    }

    private static class TaskItem {
        public final File root;
        public final File path;

        public TaskItem(File root, File path) {
            this.root = root;
            this.path = path;
        }
    }


}
