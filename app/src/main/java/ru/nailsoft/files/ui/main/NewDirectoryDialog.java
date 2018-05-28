package ru.nailsoft.files.ui.main;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.AbsHistoryItem;
import ru.nailsoft.files.model.DirectoryHistoryItem;
import ru.nailsoft.files.model.TabData;

public class NewDirectoryDialog {

    public static void show(Context context, TabData data) {
        new NewDirectoryDialog(context, data).show();
    }

    private final Context context;
    private final TabData data;
    private EditText editor;
    private AlertDialog dialog;
    private Button positive;

    private NewDirectoryDialog(Context context, TabData data) {
        this.context = context;
        this.data = data;
    }

    private void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(R.string.new_directory);
        builder.setView(R.layout.dialog_new_directory);
        builder.setPositiveButton(R.string.make, null);
        builder.setNegativeButton(R.string.cancel, null);

        dialog = builder.create();
        dialog.setOnShowListener(this::onDialogShow);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

    }

    private void onDialogShow(DialogInterface dialogInterface) {
        positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setOnClickListener(this::onMakeClick);

        editor = dialog.findViewById(R.id.edit);
        editor.selectAll();

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                positive.setEnabled(s.length() > 0);
            }
        });
    }

    private void onMakeClick(View view) {
        String name = editor.getText().toString();
        if (name.isEmpty())
            return;

        AbsHistoryItem path = data.getPath();
        if (!(path instanceof DirectoryHistoryItem))
            return;

        File file = new File(((DirectoryHistoryItem) path).path, name);
        if (file.mkdirs()) {
            data.navigate(file);
            dialog.dismiss();
            return;
        }

        if (file.exists()) {
            Toast.makeText(dialog.getContext(), "Directory already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(dialog.getContext(), "Failed to make directory", Toast.LENGTH_SHORT).show();
    }
}
