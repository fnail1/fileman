package ru.nailsoft.files.model;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.Spanned;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.Locale;

import ru.nailsoft.files.R;
import ru.nailsoft.files.toolkit.io.FileUtils;

import static ru.nailsoft.files.App.app;

public final class FileItem implements Cloneable {

    public static final FileItem EMPTY = new FileItem(new File("/dev/null"));
    public final File file;
    public final boolean hidden;
    public final boolean directory;
    public final String name;
    public final String ext;
    public final String order;
    public long size;
    public String mimeType;
    private String length = "";
    CharSequence subtitle;
    public boolean detailsResolved;

    public FileItem(File file) {
        this(file, file.isDirectory());
    }

    private FileItem(File file, boolean isDirectory) {
        this.file = file;
        directory = isDirectory;
        name = file.getName();
        hidden = name.charAt(0) == '.';
        order = (hidden ? name.substring(1) : name).toLowerCase();
        int idx = name.lastIndexOf('.');
        if (idx > 0) {
            ext = name.substring(idx);
        } else {
            ext = "";
        }

    }

    public void resolveDetails() {
        size = FileUtils.getLength(file);
        length = FileUtils.formatLength(size);
        if (!directory) {
            Uri fileUri = Uri.fromFile(file);
            mimeType = resolveMimeType(file, fileUri);
        }
        detailsResolved = true;
    }

    private static String resolveMimeType(File file, Uri url) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        if (idx >= 0) {
            String ext = name.substring(idx + 1).toLowerCase(Locale.US);
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (!TextUtils.isEmpty(type))
                return type;
        }

        String type = app().getContentResolver().getType(url);
        if (type == null)
            type = "text/*";

        return type;
//        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
//        if (extension != null) {
//            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
//        }
//        return type;
    }

    public void open(Context context) {
        Uri uri = DocumentsContract.buildDocumentUri(context.getApplicationContext().getPackageName() + ".provider", file.getAbsolutePath());

        if (mimeType == null)
            mimeType = resolveMimeType(file, uri);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file, name)));
    }

    public CharSequence getSubtitle(Resources resx) {
        if (subtitle != null)
            return subtitle;
        return resx.getString(R.string.file_size_format, length);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileItem fileItem = (FileItem) o;

        return file.equals(fileItem.file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public FileItem clone() {
        try {
            return (FileItem) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
