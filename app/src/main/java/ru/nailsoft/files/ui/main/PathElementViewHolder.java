package ru.nailsoft.files.ui.main;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class PathElementViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final MasterInterface master;
    private File file;

    public PathElementViewHolder(View itemView, MasterInterface master) {
        super(itemView);
        this.master = master;
        itemView.setOnClickListener(this);
    }

    public void bind(File file) {
        this.file = file;
        ((TextView) itemView).setText(file.getName() + " >");
    }

    @Override
    public void onClick(View v) {
        master.onPathSelected(file);
    }

    public interface MasterInterface{
        void onPathSelected(File file);
    }
}
