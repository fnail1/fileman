package ru.nailsoft.files.ui;

import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.io.File;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.ui.base.BaseActivity;
import ru.nailsoft.files.utils.ShareHelper;

public class OpenAsDialog {

    public static void show(BaseActivity context, FileItem fileItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);

        builder.setTitle(R.string.openAs);

        String[] types = context.getResources().getStringArray(R.array.open_type);
        ListAdapter adapter = new ArrayAdapter<>(context, R.layout.item_open_type, R.id.title, types);

        builder.setAdapter(adapter, (dialog, which) -> onMimeTypeSelected(context, fileItem, which));

        builder.show();
    }

    private static void onMimeTypeSelected(BaseActivity context, FileItem fileItem, int which) {
        File path = fileItem.getFile();

        switch (which) {
            case 0: // text
                ShareHelper.open(context, path, "text/*");
                break;
            case 1: // image
                ShareHelper.open(context, path, "image/*");
                break;
            case 2: // audio
                ShareHelper.open(context, path, "audio/*");
                break;
            case 3: // video
                ShareHelper.open(context, path, "video/*");
                break;
        }
    }
}
