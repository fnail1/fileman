package ru.nailsoft.files.ui.main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import ru.nailsoft.files.R;

class PathAdapter extends RecyclerView.Adapter {
    private RecyclerView list;
    private final ArrayList<File> elements = new ArrayList<>();
    private final PathElementViewHolder.MasterInterface master;

    public PathAdapter(File path, PathElementViewHolder.MasterInterface master) {
        this.master = master;
        do {
            elements.add(path);
        } while ((path = path.getParentFile()) != null);
        Collections.reverse(elements);
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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PathElementViewHolder(LayoutInflater.from(list.getContext()).inflate(R.layout.item_path_element, parent, false), master);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((PathElementViewHolder) holder).bind(elements.get(position));
    }

    @Override
    public int getItemCount() {
        return elements.size();
    }
}
