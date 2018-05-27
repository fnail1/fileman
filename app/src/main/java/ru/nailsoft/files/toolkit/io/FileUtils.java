package ru.nailsoft.files.toolkit.io;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.system.Os;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static ru.nailsoft.files.diagnostics.DebugUtils.safeThrow;
import static ru.nailsoft.files.diagnostics.Logger.logV;
import static ru.nailsoft.files.toolkit.collections.Query.query;

@SuppressWarnings("WeakerAccess")
public class FileUtils {

    public static final int MAX_BUFFER_SIZE = 6 * 1024 * 1024;
    public static final long K = 1024;
    public static final long M = K * K;
    public static final long G = K * M;
    public static final long T = K * G;

    /**
     * allocates buffer for upload and download
     * size of buffer is min of maxSize and (BitmapUtils.MAX_FILE_SIZE + BitmapUtils.FILE_SIZE_DEVIATION) or less if not enough memory.
     * if it is not enough memory to allocate 1kb throws OutOfMemoryError
     *
     * @param maxSize pass Content-Length or file size here
     */
    public static byte[] allocateBuffer(long maxSize) {

        int bufferSize = 0 < maxSize && maxSize < MAX_BUFFER_SIZE
                ? (int) maxSize
                : MAX_BUFFER_SIZE;
        for (; ; ) {
            try {
                return new byte[bufferSize];
            } catch (OutOfMemoryError e) {
                if ((bufferSize >>= 1) < 1024)
                    throw e;
            }
        }
    }

    public static boolean writeText(File file, String text) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                try {
                    writer.write(text);
                    return true;
                } finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        safeThrow(e);
                    }
                }
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    safeThrow(e);
                }
            }
        } catch (IOException e) {
            safeThrow(e);
        }
        return false;
    }

    public static boolean appendText(File file, String text) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file, true);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                try {
                    writer.write(text);
                    return true;
                } finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        safeThrow(e);
                    }
                }
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    safeThrow(e);
                }
            }
        } catch (IOException e) {
            safeThrow(e);
        }
        return false;
    }

    public static String readText(File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                int len = inputStream.available();
                char[] chars = new char[len];
                InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                try {
                    int c, idx = 0;
                    while (idx < len && (c = reader.read(chars, idx, len - idx)) >= 0)
                        idx += c;

                    return new String(chars);
                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        safeThrow(e);
                    }
                }
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    safeThrow(e);
                }
            }
        } catch (IOException e) {
            safeThrow(e);
            return null;
        }
    }

    public static byte[] readBytes(File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                int len = inputStream.available();
                byte[] bytes = new byte[len];
                int c, idx = 0;
                while (idx < len && (c = inputStream.read(bytes, idx, len - idx)) >= 0)
                    idx += c;

                return bytes;
            } finally {
                safeClose(inputStream);
            }
        } catch (IOException e) {
            safeThrow(e);
            return null;
        }
    }

    public static boolean deleteRecursive(File path) {
        if (path.isDirectory())
            for (File child : path.listFiles())
                if (!deleteRecursive(child))
                    return false;

        return path.delete();
    }

    public static void extractFiles(File zip, File dstDir) throws IOException {
        FileInputStream inputStream = new FileInputStream(zip);
        try {
            extractFiles(inputStream, dstDir);
        } finally {
            inputStream.close();
        }
    }

    private static void extractFiles(InputStream zip, File dstDir) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(zip);
        try {
            ZipEntry entry;
            byte[] buffer = new byte[16 * 1024];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File f = new File(dstDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!f.mkdirs() && !f.exists())
                        throw new IllegalArgumentException();
                } else {
                    long size = entry.getSize();
                    if (size < 0)
                        size = Integer.MAX_VALUE;
                    FileOutputStream outputStream = openFileOutputStream(f);
                    try {
                        while (size > 0) {
                            int s = (int) Math.min(size, buffer.length);
                            s = zipInputStream.read(buffer, 0, s);
                            if (s < 0)
                                break;
                            outputStream.write(buffer, 0, s);
                            size -= s;
                        }
                    } finally {
                        safeClose(outputStream);
                    }
                }
            }
        } finally {
            safeClose(zipInputStream);
        }
    }

    @NonNull
    public static FileOutputStream openFileOutputStream(File f) throws FileNotFoundException {
        return openFileOutputStream(f, false);
    }

    @NonNull
    public static FileOutputStream openFileOutputStream(File f, boolean append) throws FileNotFoundException {
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(f, append);
        } catch (FileNotFoundException e) {
            if (f.getParentFile().mkdirs())
                outputStream = new FileOutputStream(f, false);
            else
                throw e;
        }
        return outputStream;
    }

    public static List<File> listFiles(File src) {
        ArrayList<File> out = new ArrayList<>();
        out.add(src);
        listFiles(src, out);
        return out;
    }

    public static void listFiles(File dir, ArrayList<File> out) {
        if (!dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            out.add(file);
            listFiles(file, out);
        }
    }

    public static void rename(File from, File to) throws IOException, FileOpException {
        if (!from.renameTo(to)) {
            if (!from.exists()) {
                throw new FileNotFoundException("Unable to rename. File does not exists: " + from.getAbsolutePath());
            }

            if (from.isDirectory()) {
                if (to.exists()) {
                    if (!to.isDirectory())
                        throw new IOException("Can't move directory to file", new Exception(from + " -> " + to));
                } else if (!to.mkdirs()) {
                    throw new FileOpException(FileOpException.FileOp.MKDIR, to);
                }

                for (File file : from.listFiles()) {
                    rename(file, new File(to, file.getName()));
                }

                if (!from.delete())
                    throw new FileOpException(FileOpException.FileOp.DELETE, from);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        Os.rename(from.getAbsolutePath(), to.getAbsolutePath());
                        return;
                    } catch (Exception ignored) {
                    }
                }

                copy(from, to, true);
                //noinspection ResultOfMethodCallIgnored
                from.delete();
            }
        }
    }

    public static void copy(File src, File dst, boolean override) throws FileOpException, IOException {
        if (!src.isDirectory()) {
            if (dst.exists()) {
                if (dst.isDirectory())
                    dst = new File(dst, src.getName());
                else if (!override)
                    throw new FileOpException(FileOpException.FileOp.RENAME, src, dst);
            }
            copyFile(src, dst);
        }

        dst = new File(dst, src.getName());

        if (!dst.exists() && !dst.mkdirs())
            throw new FileOpException(FileOpException.FileOp.MKDIR, dst);

        for (File file : src.listFiles()) {
            copy(file, dst, override);
        }

    }

    public static void copyFile(final File src, final File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src)) {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                final byte[] buf = new byte[(int) Math.min(32 * 1024, src.length())];

                int r;
                while ((r = fis.read(buf)) >= 0) {
                    fos.write(buf, 0, r);
                }
                fos.getFD().sync();
            }
        }

    }

    public static boolean copyFile(final File src, final File dest, int from, int to) {
        try {
            FileInputStream fis = new FileInputStream(src);
            try {
                FileOutputStream fos = new FileOutputStream(dest);
                try {
                    final byte[] buf = new byte[(int) Math.min(32 * 1024, src.length())];

                    if (from > 0)
                        fis.skip(from);

                    while (true) {
                        int c = Math.min(buf.length, to - from);
                        if (c <= 0)
                            break;
                        int r = fis.read(buf, 0, c);
                        if (r == 0)
                            break;
                        from += r;
                        fos.write(buf, 0, r);
                    }
                    return true;
                } finally {
                    try {
                        fos.getFD().sync();
                        fos.close();
                    } catch (IOException e) {
                        logV("IO", e.getMessage());
                    }
                }

            } finally {
                safeClose(fis);
            }
        } catch (IOException ignored) {
            logV("IO", ignored.getMessage());
            return false;
        }
    }

    public static boolean compare(File a, File b) {
        byte[] ba = readBytes(a);
        byte[] bb = readBytes(b);
        return Arrays.equals(ba, bb);
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        copyStream(inputStream, outputStream, allocateBuffer(64 * 1024));
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream, byte[] buf) throws IOException {
        int count;
        while ((count = inputStream.read(buf)) >= 0) {
            outputStream.write(buf, 0, count);
        }
    }

    public static void safeClose(Closeable obj) {
        try {
            obj.close();
        } catch (IOException e) {
            safeThrow(e);
        }
    }

    public static void zip(File file, File zipFile) {
        try {
            FileOutputStream dest = new FileOutputStream(zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            try {
                byte data[] = new byte[16 * 1024];

                zipEntry(file.getParentFile(), file, out, data);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void zipEntry(File root, File file, ZipOutputStream out, byte[] buffer) throws IOException {
        if (file.isDirectory()) {
            ZipEntry entry = new ZipEntry(file.getAbsolutePath().substring(root.getAbsolutePath().length()) + '/');
            out.putNextEntry(entry);
            for (File child : file.listFiles())
                zipEntry(root, child, out, buffer);
            return;
        }

        FileInputStream fi = new FileInputStream(file);
        BufferedInputStream origin = new BufferedInputStream(fi, 16 * 1024);
        try {
            ZipEntry entry = new ZipEntry(file.getAbsolutePath().substring(root.getAbsolutePath().length()));
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, count);
            }
        } finally {
            origin.close();
        }
    }

    public static long getLength(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null)
                return 0;
            long length = query(files).longSum(FileUtils::getLength);
            if (length < 0) {
                logV("getLength", "" + length);
                List<Long> longs = query(files).select(File::length).toList();
                length = 0;
            }
            return length;
        } else {
            long length = file.length();
            if (length < 0)
                length = 0;
            return length;
        }
    }

    public static String formatLength(long length) {
        if (length > T)
            return String.valueOf((float) (length * 100 / T) / 100) + " Tb";
        if (length > G)
            return String.valueOf((float) (length * 100 / G) / 100) + " Gb";
        if (length > M)
            return String.valueOf((float) (length * 10 / M) / 10) + " Mb";
        if (length > K)
            return String.valueOf((float) (length * 10 / K) / 10) + " Kb";

        return String.valueOf(length) + " b";
    }

    public static Drawable resolveFileIcon() {
        return null;
    }

    @NonNull
    public static String replaceExt(@NonNull String name, @NonNull String ext) {
        if (ext.charAt(0) != '.')
            ext = '.' + ext;

        int ptidx = name.lastIndexOf('.');
        int s1idx = name.lastIndexOf('\\');
        int s2idx = name.lastIndexOf('/');
        if (ptidx > 0 && ptidx > s1idx && ptidx > s2idx)
            return name.substring(0, ptidx) + ext;
        return name + ext;
    }

    public static List<File> searchRecursive(File root, String filter) {
        ArrayList<File> out = new ArrayList<>();
        searchRecursive(root, filter.toLowerCase(), out);
        return out;
    }

    private static void searchRecursive(File root, String filter, ArrayList<File> out) {
        for (File file : root.listFiles()) {
            if (file.getName().toLowerCase().contains(filter)) {
                out.add(file);
            }
            if (file.isDirectory())
                searchRecursive(file, filter, out);
        }
    }
}
