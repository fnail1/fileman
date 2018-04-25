package ru.nailsoft.files.ui.main;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.MainActivityData;
import ru.nailsoft.files.model.TabData;


class TabsTitlesAdapter extends RecyclerView.Adapter {

    private final MasterInterface master;
    private final MainActivityData data;
    private RecyclerView list;

    TabsTitlesAdapter(MasterInterface master, MainActivityData data) {
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
        if (data.tabs.get(position) == master.currentTab())
            return R.layout.item_tab_selected;
        else
            return R.layout.item_tab;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case R.layout.item_tab:
            case R.layout.item_tab_selected:
                return new TabViewHolder(list, parent, viewType, master);
        }
        throw new IllegalArgumentException("" + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case R.layout.item_tab:
                ((TabViewHolder) holder).bind(data.tabs.get(position), false);
                break;
            case R.layout.item_tab_selected:
                ((TabViewHolder) holder).bind(data.tabs.get(position), true);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return data.tabs.size();
    }

    public interface MasterInterface extends TabViewHolder.MasterInterface {

        TabData currentTab();
    }
}
