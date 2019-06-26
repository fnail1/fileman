package ru.nailsoft.files.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.Collections;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.ui.base.BaseActivity;

import static ru.nailsoft.files.App.clipboard;

public class ExtractDialogFragment extends AppCompatDialogFragment implements DialogInterface.OnShowListener, CompoundButton.OnCheckedChangeListener {

    private static final String ARG_FILE = "arg_file";

    @BindView(R.id.title) TextView title;
    @BindView(R.id.root) LinearLayout root;
    @BindView(R.id.cut) RadioButton cut;
    @BindView(R.id.copy) RadioButton copy;
    @BindView(R.id.subtitle) TextView subtitle;
    Unbinder unbinder1;
    private Unbinder unbinder;
    private Button buttonPositive;
    private FileItem file;

    public static void show(BaseActivity context, FileItem file) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);

        ExtractDialogFragment fragment = new ExtractDialogFragment();
        fragment.setArguments(args);

        context.getSupportFragmentManager().beginTransaction()
                .add(fragment, "ExtractDialogFragment")
                .commit();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setTitle(R.string.extracting);
        builder.setView(R.layout.dialog_unarchive);
        builder.setPositiveButton("ok", this::onPositiveButtonClick);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(this);

        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog alertDialog = (AlertDialog) dialog;
        View rootView = Objects.requireNonNull(alertDialog.findViewById(R.id.root));
        unbinder = ButterKnife.bind(this, rootView);
        buttonPositive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);

        Bundle args = Objects.requireNonNull(getArguments());
        file = Objects.requireNonNull(args.getParcelable(ARG_FILE));
        title.setText(file.name);

        copy.setOnCheckedChangeListener(this);
        if (file.readOnly) {
            cut.setVisibility(View.GONE);
            copy.setVisibility(View.GONE);
            subtitle.setVisibility(View.GONE);
        } else {
            cut.setOnCheckedChangeListener(this);
        }
        copy.setChecked(true);
        onCheckedChanged(copy, true);
    }

    private void onPositiveButtonClick(DialogInterface dialogInterface, int button) {
        clipboard().addAll(Collections.singleton(file), !file.readOnly && cut.isChecked(), true);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked)
            return;

        if (copy == buttonView) {
            cut.setChecked(false);
            buttonPositive.setText(R.string.copy_content);
        } else {
            copy.setChecked(false);
            buttonPositive.setText(R.string.cut_content);
        }
    }
}
