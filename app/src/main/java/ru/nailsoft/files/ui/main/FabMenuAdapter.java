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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.service.ClipboardItem;
import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;

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
            case R.layout.item_fab_menu_simple:
                return new SimpleItemViewHolder(parent);
            case R.layout.item_fab_menu_separator:
                return new ItemViewHolder(inflater.inflate(viewType, parent, false));
            case R.layout.item_fab_menu_expandable:
                return new ExpandableItemViewHolder(parent);
        }
        throw new IllegalArgumentException(" " + viewType);
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

    protected class ExpandableItemViewHolder extends SimpleItemViewHolder
            implements View.OnClickListener {
        private ExpandableItem item;

        @BindView(R.id.expand)
        ImageView expand;


        ExpandableItemViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_fab_menu_expandable, parent, false));
        }

        @Override
        public void bind(Item item) {
            super.bind(item);
        }

        @Override
        public void onClick(View v) {
            if (v == expand)
                item.onExpand();
            else
                super.onClick(v);
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
            super(file.file.name, file.file.file.getAbsolutePath());
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

            if (file.removeSource) {
                try {
                    FileUtils.rename(file.file.file, new File(callback.currentTab().getPath(), file.file.file.getName()));
                } catch (IOException | FileOpException e) {
                    e.printStackTrace();
                }
            } else {

                try {
                    FileUtils.copy(file.file.file, new File(callback.currentTab().getPath(), file.file.file.getName()), false);
                } catch (FileOpException e) {
                    e.printStackTrace();
                    if (e.op == FileOpException.FileOp.RENAME && e.dstExist) {

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public static abstract class ExpandableItem extends SimpleItem {


        ExpandableItem(String title, String subtitle) {
            super(title, subtitle);
        }

        @Override
        public int viewType() {
            return R.layout.item_fab_menu_expandable;
        }


        public abstract void onExpand();
    }

    public static class DirectoryItem extends ExpandableItem {

        private final ClipboardItem file;

        DirectoryItem(ClipboardItem file) {
            super(file.file.name, file.file.file.getAbsolutePath());

            this.file = file;
        }

        public ClipboardItem getFile() {
            return file;
        }

        @Override
        public void bindIcon(ImageView imageView) {
            icons().attach(imageView, file.file)
                    .size(screenMetrics().menuIcon)
                    .commit();
        }

        @Override
        public void onClick() {
            callback.openFile(file.file);
        }

        @Override
        public void onExpand() {
            callback.expandBufferedDirectoryExpand(file);
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
        SelectAllItem() {
            super("Select All", null, R.drawable.ic_done_all_primary);
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

    public static class CloseItem extends CommonItem {
        CloseItem(Context context) {
            super(context.getResources().getString(R.string.close), null, R.drawable.ic_close_primary);
        }

        @Override
        public void onClick() {
            callback.closeFabMenu();
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

        void expandBufferedDirectoryExpand(ClipboardItem file);

        void closeFabMenu();

        void pasteAll();

        void clearClipboard();

        TabData currentTab();

        void openFile(ru.nailsoft.files.model.FileItem file);
    }
}
