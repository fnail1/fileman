package ru.nailsoft.files.ui.main.pages;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.model.FileItem;

class TabAdapter extends RecyclerView.Adapter {
    private final FileViewHolder.MasterInterface master;
    private final TabData data;
    private RecyclerView list;

    public TabAdapter(TabData data, FileViewHolder.MasterInterface master) {
        this.master = master;
        this.data = data;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        list = recyclerView;
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        list = null;
    }

    @Override
    public int getItemViewType(int position) {
        FileItem fi = data.files.get(position);
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((FileViewHolder) holder).bind(data.files.get(position));
    }

    @Override
    public int getItemCount() {
        return data.files.size();
    }

}
