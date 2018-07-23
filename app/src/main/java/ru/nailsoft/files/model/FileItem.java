package ru.nailsoft.files.model;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.Locale;

import ru.nailsoft.files.R;
import ru.nailsoft.files.toolkit.io.FileUtils;

import static ru.nailsoft.files.App.app;
import static ru.nailsoft.files.diagnostics.Logger.trace;

public final class FileItem implements Cloneable, Parcelable {

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
    public CharSequence title;
    public CharSequence subtitle;
    public boolean detailsResolved;
    public boolean readOnly;

    public FileItem(File file) {
        this(file, file.isDirectory());
    }

    private FileItem(File file, boolean isDirectory) {
        this.file = file;
        directory = isDirectory;
        title = name = file.getName();
        hidden = name.charAt(0) == '.';
        order = (hidden ? name.substring(1) : name).toLowerCase();
        int idx = name.lastIndexOf('.');
        if (idx > 0) {
            ext = name.substring(idx);
        } else {
            ext = "";
        }

    }

    public FileItem(Uri data) {
        file = new File(data.getPath());
        directory = false;
        String path = data.getPath();
        title = name = path.substring(path.lastIndexOf("/"));
        hidden = false;
        order = name;
        int idx = name.lastIndexOf('.');
        if (idx > 0) {
            ext = name.substring(idx);
        } else {
            ext = "";
        }
        readOnly = true;
    }

    protected FileItem(Parcel in) {
        file = new File(in.readString());
        hidden = in.readByte() != 0;
        directory = in.readByte() != 0;
        title = name = in.readString();
        ext = in.readString();
        order = in.readString();
        size = in.readLong();
        mimeType = in.readString();
        length = in.readString();
        detailsResolved = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(file.getAbsolutePath());
        dest.writeByte((byte) (hidden ? 1 : 0));
        dest.writeByte((byte) (directory ? 1 : 0));
        dest.writeString(name);
        dest.writeString(ext);
        dest.writeString(order);
        dest.writeLong(size);
        dest.writeString(mimeType);
        dest.writeString(length);
        dest.writeByte((byte) (detailsResolved ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileItem> CREATOR = new Creator<FileItem>() {
        @Override
        public FileItem createFromParcel(Parcel in) {
            return new FileItem(in);
        }

        @Override
        public FileItem[] newArray(int size) {
            return new FileItem[size];
        }
    };

    public void resolveDetails() {
        if (name.startsWith("846")) {
            trace(name);
        }
        size = FileUtils.getLength(file);
        length = FileUtils.formatLength(size);
        if (!directory) {
            Uri fileUri = Uri.fromFile(file);
            mimeType = resolveMimeType(file, fileUri);
        }
        detailsResolved = true;
//        trace("%s %s", name, mimeType);
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
    }


    public CharSequence getSubtitle(Resources resx) {
        if (name.startsWith("846")) {
            trace();
        }
        if (subtitle != null)
            return subtitle;
        if (detailsResolved)
            return resx.getString(R.string.file_size_format, length);
        else
            return "processing...";
    }

    public boolean isArchive() {
        switch (ext.toLowerCase()) {
            case ".zip":
                return true;
        }
        return false;
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

    public long lastModified() {
        return file.lastModified();
    }

    public String id() {
        return file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

    public Uri getUri() {
        return DocumentsContract.buildDocumentUri(app().getApplicationContext().getPackageName() + ".provider", file.getAbsolutePath());
    }
}
