package ru.nailsoft.files.ui.main.pages;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.model.FileItem;

class TabAdapter extends RecyclerView.Adapter {
    private final FileViewHolder.MasterInterface master;
    private final TabData data;
    private RecyclerView list;

    TabAdapter(TabData data, FileViewHolder.MasterInterface master) {
        this.master = master;
        this.data = data;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        list = recyclerView;
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        list = null;
    }

    @Override
    public int getItemViewType(int position) {
        FileItem fi = data.displayFiles.get(position);
        if (fi.directory) {
            if (fi.hidden)
                return R.layout.item_directory_hidden;
            else
                return R.layout.item_directory;
        } else {
            if (fi.hidden)
                return R.layout.item_file_hidden;
            else
                return R.layout.item_file;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case R.layout.item_file:
            case R.layout.item_file_hidden:
            case R.layout.item_directory:
            case R.layout.item_directory_hidden:
                return new FileViewHolder(list, parent, viewType, master);
        }
        throw new IllegalArgumentException("" + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((FileViewHolder) holder).bind(data.displayFiles.get(position));
    }

    @Override
    public int getItemCount() {
        return data.displayFiles.size();
    }

}
