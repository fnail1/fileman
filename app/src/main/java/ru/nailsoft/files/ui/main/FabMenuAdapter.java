package ru.nailsoft.files.ui.main;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.service.ClipboardItem;

import static ru.nailsoft.files.App.icons;
import static ru.nailsoft.files.App.screenMetrics;

public class FabMenuAdapter extends RecyclerView.Adapter {

    private List<Item> data = Collections.emptyList();
    private RecyclerView list;
    private Context context;
    private LayoutInflater inflater;
    private final MasterInterface master;

    FabMenuAdapter(MasterInterface master) {
        this.master = master;
    }

    public void setItems(List<Item> items) {
        data = items;
        notifyDataSetChanged();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        list = recyclerView;
        context = list.getContext();
        inflater = LayoutInflater.from(context);
        super.onAttachedToRecyclerView(recyclerView);

    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        inflater = null;
        context = null;
        list = null;
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).viewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case R.layout.item_empty_space:
                return new HeaderSupportViewHolder(inflater.inflate(viewType, parent, false));
            case R.layout.item_fab_menu_simple:
                return new SimpleItemViewHolder(parent);
            case R.layout.item_fab_menu_separator:
                return new ItemViewHolder(inflater.inflate(viewType, parent, false));
        }
        throw new IllegalArgumentException(" " + viewType);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        switch (holder.getItemViewType()) {
            case R.layout.item_empty_space:
                ((HeaderSupportViewHolder) holder).onAttach();
                break;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        switch (holder.getItemViewType()) {
            case R.layout.item_empty_space:
                ((HeaderSupportViewHolder) holder).onDetach();
                break;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = data.get(position);
        item.setCallback(master);
        ((ItemViewHolder) holder).bind(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {

        ItemViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(Item item) {
        }
    }

    protected class SimpleItemViewHolder extends ItemViewHolder
            implements View.OnClickListener {

        private SimpleItem item;
        @BindView(R.id.icon)
        ImageView icon;

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.subtitle)
        TextView subtitle;

        SimpleItemViewHolder(ViewGroup parent) {
            this(inflater.inflate(R.layout.item_fab_menu_simple, parent, false));
        }

        SimpleItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Item item) {
            this.item = (SimpleItem) item;

            this.item.bindIcon(icon);

            title.setText(this.item.getTitle());
            String subtitle = this.item.getSubtitle();
            if (subtitle != null) {
                this.subtitle.setText(subtitle);
                this.subtitle.setVisibility(View.VISIBLE);
            } else {
                this.subtitle.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            item.onClick();
        }

    }

    private class HeaderSupportViewHolder extends ItemViewHolder {

        private HeaderSupportItem item;
        private final MyHeaderLayoutListener headerLayoutListener;

        public HeaderSupportViewHolder(View itemView) {
            super(itemView);
            headerLayoutListener = new MyHeaderLayoutListener();
        }

        @Override
        public void bind(Item item) {
            super.bind(item);
            this.item = ((HeaderSupportItem) item);
        }

        public void onAttach() {
            item.headerView.addOnLayoutChangeListener(headerLayoutListener);
        }

        public void onDetach() {
            item.headerView.removeOnLayoutChangeListener(headerLayoutListener);
        }

        private class MyHeaderLayoutListener implements View.OnLayoutChangeListener {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.width = right - left;
                layoutParams.height = bottom - top;
                itemView.setLayoutParams(layoutParams);
            }
        }
    }

    public static abstract class Item {

        MasterInterface callback;

        @LayoutRes
        public abstract int viewType();

        void setCallback(MasterInterface callback) {
            this.callback = callback;
        }

    }

    public static class HeaderSupportItem extends Item {
        private final View headerView;

        public HeaderSupportItem(View root) {
            super();
            headerView = root;
        }

        @Override
        public int viewType() {
            return R.layout.item_empty_space;
        }

    }

    public static class SeparatorItem extends Item {

        @Override
        public int viewType() {
            return R.layout.item_fab_menu_separator;
        }
    }

    public static abstract class SimpleItem extends Item {

        private final String title;
        private final String subtitle;

        SimpleItem(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }


        @Override
        public int viewType() {
            return R.layout.item_fab_menu_simple;
        }

        public abstract void bindIcon(ImageView imageView);

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public abstract void onClick();
    }

    public static class FileItem extends SimpleItem {

        protected final ClipboardItem file;

        public FileItem(ClipboardItem file) {
            super(file.file.name, file.file.file.getParent());
            this.file = file;
        }

        @Override
        public void bindIcon(ImageView imageView) {
            icons().attach(imageView, file.file)
                    .size(screenMetrics().menuIcon)
                    .commit();
        }

        @Override
        public void onClick() {
            callback.paste(file);
        }
    }

    public static abstract class CommonItem extends SimpleItem {

        private final int icon;

        CommonItem(String title, String subtitle, @DrawableRes int icon) {
            super(title, subtitle);
            this.icon = icon;
        }

        @Override
        public void bindIcon(ImageView imageView) {
            imageView.setImageResource(icon);
        }
    }

    public static class SelectAllItem extends CommonItem {
        SelectAllItem(Context context) {
            super(context.getResources().getString(R.string.select_all), null, R.drawable.ic_done_all_primary);
        }

        @Override
        public void onClick() {
            callback.selectAll();
        }
    }

    public static class NewDirectoryItem extends CommonItem {
        NewDirectoryItem(Context context) {
            super(context.getResources().getString(R.string.new_directory), null, R.drawable.ic_add_primary);
        }

        @Override
        public void onClick() {
            callback.createNewDirectory();
        }
    }

    public static class PasteAllItem extends CommonItem {
        PasteAllItem(Context context) {
            super(context.getString(R.string.pasteAll), null, R.drawable.ic_paste);
        }

        @Override
        public void onClick() {
            callback.pasteAll();
        }
    }

    public static class ClearItem extends CommonItem {

        ClearItem(Context context) {
            super(context.getResources().getString(R.string.clear), null, R.drawable.ic_clear);
        }

        @Override
        public void onClick() {
            callback.clearClipboard();
        }

    }

    public interface MasterInterface {


        void selectAll();

        void createNewDirectory();

        void closeFabMenu();

        void pasteAll();

        void clearClipboard();

        TabData currentTab();

        void paste(ClipboardItem file);

    }
}
