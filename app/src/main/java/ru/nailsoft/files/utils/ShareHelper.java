package ru.nailsoft.files.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.ui.base.BaseActivity;

import static ru.nailsoft.files.toolkit.collections.Query.query;

public class ShareHelper {
    public static void share(BaseActivity context, Collection<FileItem> files) {
        if (files.size() == 1) {
            FileItem fileItem = files.iterator().next();
            share(context, fileItem.file, fileItem.mimeType);
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

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.error_share_app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private static String compositeMimeType(Collection<FileItem> items) {
        Iterator<FileItem> iterator = items.iterator();
        String mimeType = iterator.next().mimeType;
        if (mimeType == null)
            return null;
        while (iterator.hasNext()) {
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

    public static void open(Context context, File path, String mimeType) {
        context = context.getApplicationContext();
        Uri uri = DocumentsContract.buildDocumentUri(context.getPackageName() + ".provider", path.getAbsolutePath());

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }

        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file, path.getName())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.error_share_app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    public static void share(Context context, File file, String mimeType) {
        context = context.getApplicationContext();
        Uri uri = DocumentsContract.buildDocumentUri(context.getPackageName() + ".provider", file.getAbsolutePath());

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType(mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.error_share_app_not_found, Toast.LENGTH_SHORT).show();
        }
    }
}
