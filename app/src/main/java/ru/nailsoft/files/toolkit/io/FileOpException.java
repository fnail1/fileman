package ru.nailsoft.files.toolkit.io;

import android.support.annotation.NonNull;

import java.io.File;

public class FileOpException extends Exception {
    public final FileOp op;
    public final String srcPath;
    public final boolean srcExist;
    public final String dstPath;
    public final boolean dstExist;

    public FileOpException(FileOp op, File file) {
        super("Failed to " + op, new Exception(descrFile(file)));
        this.op = op;
        dstPath = srcPath = file.getPath();
        dstExist = srcExist = file.exists();
    }

    public FileOpException(FileOp op, File src, File dst) {
        super("Failed to " + op, new Exception(descrFile(src) + ", " + descrFile(dst)));
        this.op = op;
        srcPath = src.getPath();
        srcExist = src.exists();
        dstPath = dst.getPath();
        dstExist = dst.exists();
    }

    @NonNull
    private static String descrFile(File f) {
        return f.getAbsolutePath() + " (" + (f.exists() ? "exist" : "not exist") + ")";
    }

    public enum FileOp {
        DELETE, RENAME, MKDIR
    }
}
