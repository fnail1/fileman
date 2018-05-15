package ru.nailsoft.files.ui;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ru.nailsoft.files.R;
import ru.nailsoft.files.service.CopyTask;

import static ru.nailsoft.files.App.copy;

public class CopyDialogFragment extends DialogFragment implements DialogInterface.OnShowListener {

    @BindView(R.id.progress_text) TextView progressText;
    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.file) TextView file;
    Unbinder unbinder;

    public static void show(BaseActivity context) {
        CopyDialogFragment fragment = new CopyDialogFragment();
        context.getSupportFragmentManager().beginTransaction()
                .add(fragment, "CopyDialogFragment")
                .commit();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.moving_files);
        builder.setView(R.layout.dialog_progress);

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(this);
        return alertDialog;
    }


    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog alertDialog = (AlertDialog) dialog;
        View rootView = alertDialog.findViewById(R.id.root);
        unbinder = ButterKnife.bind(this, rootView);
        onTickTimer();
    }

    private void onTickTimer() {
        if (progressText == null)
            return;

        CopyTask task = copy().getCurrentTask();
        if (task != null) {
            if (task.getState() == CopyTask.State.COMPLETE) {
                dismiss();
                return;
            }

            progress.setIndeterminate(false);
            progress.setMax(task.getCount());
            progress.setProgress(task.getProgress());
            switch (task.getState()) {
                case NEW:
                    progressText.setText("");
                    break;
                case ANALIZE:
                    progressText.setText("Analize");
                    break;
                case PROGRESS:
                    progressText.setText(String.format("%d of %d", task.getProgress(), task.getCount()));
                    break;
                case FINALIZE:
                    progressText.setText("Finalize");
                    break;
                case COMPLETE:
                    progressText.setText("Complete");
                    break;
                case FAIL:
                    progressText.setText("Error");
                    break;
            }
            file.setText(task.getCurrentFile());
        } else {
            progress.setIndeterminate(true);
            file.setText("");
            progressText.setText("");
        }
        progressText.postDelayed(this::onTickTimer, 100);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
