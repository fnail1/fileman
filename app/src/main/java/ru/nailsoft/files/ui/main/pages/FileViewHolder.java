package ru.nailsoft.files.ui.main.pages;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.service.ClipboardItem;

import static ru.nailsoft.files.App.clipboard;
import static ru.nailsoft.files.App.icons;
import static ru.nailsoft.files.App.screenMetrics;

public class FileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        View.OnLongClickListener {

    @BindView(R.id.icon)
    ImageView icon;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.subtitle)
    TextView subtitle;

    @BindView((R.id.action))
    ImageView action;

    private final MasterInterface master;
    private FileItem file;

    FileViewHolder(RecyclerView list, ViewGroup parent, @LayoutRes int viewType, MasterInterface master) {
        super(LayoutInflater.from(list.getContext()).inflate(viewType, parent, false));
        this.master = master;
        ButterKnife.bind(this, itemView);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        action.setOnClickListener(this);
    }

    public void bind(FileItem file) {
        this.file = file;
        title.setText(file.name);
        icons().attach(icon, file)
                .size(screenMetrics().icon)
                .round(screenMetrics().iconRoundRadius, screenMetrics().iconRoundRadius)
                .commit();

        if (file.detailsResolved) {
            subtitle.setVisibility(View.VISIBLE);
            subtitle.setText(file.getSubtitle(subtitle.getResources()));
        } else {
            subtitle.setVisibility(View.GONE);
        }

        ClipboardItem clipboardItem = clipboard().get(file);
        if (clipboardItem != null) {
            if (clipboardItem.removeSource)
                action.setImageResource(R.drawable.ic_cut);
            else
                action.setImageResource(R.drawable.ic_copy);
        } else if (master.isSelected(file))
            action.setImageResource(R.drawable.ic_check);
        else
            action.setImageResource(R.drawable.ic_expand_more);
    }

    @Override
    public void onClick(View v) {
        if ((v == action) || (v == itemView && master.isActionMode())) {
            toggleSelection();
        } else if (v == itemView) {
            master.openFile(file);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        toggleSelection();
        return true;
    }

    private void toggleSelection() {
        boolean selected = master.toggleSelection(file);
        action.setImageResource(selected ? R.drawable.ic_check : R.drawable.ic_expand_more);
    }

    public interface MasterInterface {
        void openFile(FileItem file);

        boolean isActionMode();

        boolean toggleSelection(FileItem file);

        boolean isSelected(FileItem file);
    }
}
