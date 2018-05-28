package ru.nailsoft.files.ui.main;

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
import ru.nailsoft.files.model.TabData;

class TabViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.subtitle)
    TextView subtitle;

    private final MasterInterface master;
    private TabData data;

    public TabViewHolder(RecyclerView list, ViewGroup parent, @LayoutRes int layoutResId, MasterInterface master) {
        super(LayoutInflater.from(list.getContext()).inflate(layoutResId, parent, false));
        this.master = master;

        ButterKnife.bind(this, itemView);
        itemView.setOnClickListener(this);
    }

    public void bind(TabData tabData, boolean selected) {
        data = tabData;
        title.setText(tabData.title);
        subtitle.setText(tabData.getPath().subtitle());

        title.setSelected(selected);
        //из-за этого строка поиска теряет фокус
//        if(selected)
//            title.requestFocus();
    }

    @Override
    public void onClick(View v) {
        if (v == itemView) {
            master.onTabClick(data);
        }
    }

    public interface MasterInterface {
        void onTabClick(TabData tab);
    }
}
