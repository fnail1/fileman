package ru.nailsoft.files.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.Objects;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.ui.base.BaseActivity;
import ru.nailsoft.files.utils.ShareHelper;

public class OpenAsDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {


    private static final String ARG_FILE = "path";

    public static void show(BaseActivity context, FileItem fileItem) {
        OpenAsDialogFragment fragment = new OpenAsDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileItem);
        fragment.setArguments(args);

        context.getSupportFragmentManager().beginTransaction()
                .add(fragment, "OpenAsDialogFragment")
                .commit();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity context = Objects.requireNonNull(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);

        builder.setTitle(R.string.openAs);

        String[] types = getResources().getStringArray(R.array.open_type);
        ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.item_open_type, R.id.title, types);

        builder.setAdapter(adapter, this);

        AlertDialog alertDialog = builder.create();
        return alertDialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        FragmentActivity context = Objects.requireNonNull(getActivity());
        Bundle args = Objects.requireNonNull(getArguments());
        FileItem file = args.getParcelable(ARG_FILE);
        FileItem fileItem = Objects.requireNonNull(file);
        String path = fileItem.file.getAbsolutePath();

        switch (which) {
            case 0: // text
                ShareHelper.share(context, path, "text/*", ShareHelper.OpenMode.OPEN);
                break;
            case 1: // image
                ShareHelper.share(context, path, "image/*", ShareHelper.OpenMode.OPEN);
                break;
            case 2: // audio
                ShareHelper.share(context, path, "audio/*", ShareHelper.OpenMode.OPEN);
                break;
            case 3: // video
                ShareHelper.share(context, path, "video/*", ShareHelper.OpenMode.OPEN);
                break;
        }
    }
}
