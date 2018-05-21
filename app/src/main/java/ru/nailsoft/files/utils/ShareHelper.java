package ru.nailsoft.files.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;

import static ru.nailsoft.files.toolkit.collections.Query.query;

public class ShareHelper {
    public static void share(Context context, Collection<FileItem> files) {
        if (files.size() == 1) {
            FileItem fileItem = files.iterator().next();
            share(context, fileItem.file.getAbsolutePath(), fileItem.mimeType, OpenMode.SHARE);
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_SUBJECT, files.size() + " files");
        String mimeType = compositeMimeType(files);
        if (mimeType != null) {
            intent.setType(mimeType);
        }

        String authority = context.getApplicationContext().getPackageName() + ".provider";
        ArrayList<Uri> uris = (ArrayList<Uri>) query(files).select(f -> DocumentsContract.buildDocumentUri(authority, f.file.getAbsolutePath())).toList();
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }

        context.startActivity(intent);

    }

    private static String compositeMimeType(Collection<FileItem> items) {
        Iterator<FileItem> iterator = items.iterator();
        String mimeType = iterator.next().mimeType;
        if (mimeType == null)
            return null;
        while (iterator.hasNext()){
            String item = iterator.next().mimeType;
            if (!mimeType.equals(item)) {
                mimeType = null;
                break;
            }
        }

        if (mimeType != null)
            return mimeType;

        iterator = items.iterator();
        mimeType = iterator.next().mimeType;

        int idx = mimeType.indexOf('/');
        if (idx <= 0)
            return null;
        int l = mimeType.length();
        mimeType = mimeType.substring(0, idx);
        while (iterator.hasNext()) {

            String item = iterator.next().mimeType;

            if (item == null)
                return null;

            if (item.length() < l)
                return null;

            if (!item.startsWith(mimeType))
                return null;
        }
        return mimeType + '*';
    }

    public static void share(Context context, String path, String mimeType, OpenMode action) {
        Uri uri = DocumentsContract.buildDocumentUri(context.getApplicationContext().getPackageName() + ".provider", path);

        Intent intent = new Intent();
        switch (action) {
            case OPEN:
                intent.setAction(Intent.ACTION_VIEW);
                break;
            case SHARE:
                intent.setAction(Intent.ACTION_SEND);
                break;
        }
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file, new File(path).getName())));
    }

    public enum OpenMode {
        SHARE, OPEN
    }
}
