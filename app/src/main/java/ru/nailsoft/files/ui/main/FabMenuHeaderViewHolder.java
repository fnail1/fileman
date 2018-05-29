package ru.nailsoft.files.ui.main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.nailsoft.files.R;
import ru.nailsoft.files.utils.GraphicUtils;

import static ru.nailsoft.files.App.clipboard;
import static ru.nailsoft.files.diagnostics.Logger.trace;

public class FabMenuHeaderViewHolder implements View.OnLayoutChangeListener {
    public final View root;
    private final FabMenuAdapter.MasterInterface master;

    private int scrollOffset;
    private boolean optimized;


    @BindView(R.id.new_dir_collapsed) ImageView newDirCollapsed;
    @BindView(R.id.select_all_collapsed) ImageView selectAllCollapsed;
    @BindView(R.id.paste_all_collapsed) ImageView pasteAllCollapsed;
    @BindView(R.id.clear_collapsed) ImageView clearCollapsed;
    @BindView(R.id.new_dir) TextView newDir;
    @BindView(R.id.select_all) TextView selectAll;
    @BindView(R.id.separator1) ImageView separator1;
    @BindView(R.id.paste_all) TextView pasteAll;
    @BindView(R.id.clear) TextView clear;
    @BindView(R.id.separator2) ImageView separator2;
    @BindView(R.id.container) LinearLayout container;
    @BindView(R.id.collapsed_container) LinearLayout collapsedContainer;

    public FabMenuHeaderViewHolder(RecyclerView list, FabMenuAdapter.MasterInterface master) {
        this(list, LayoutInflater.from(list.getContext()).inflate(R.layout.item_fab_header, (ViewGroup) list.getParent()), master);
    }

    FabMenuHeaderViewHolder(RecyclerView list, View view, FabMenuAdapter.MasterInterface master) {
        root = view;
        ButterKnife.bind(this, root);
        list.addOnScrollListener(new MyScrollListener());
        root.addOnLayoutChangeListener(this);
        this.master = master;
        onListScroll(0);
    }

    @OnClick({R.id.new_dir_collapsed, R.id.select_all_collapsed, R.id.paste_all_collapsed, R.id.clear_collapsed, R.id.new_dir, R.id.select_all, R.id.paste_all, R.id.clear})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.new_dir_collapsed:
            case R.id.new_dir:
                master.createNewDirectory();
                break;
            case R.id.select_all_collapsed:
            case R.id.select_all:
                master.selectAll();
                break;
            case R.id.paste_all_collapsed:
            case R.id.paste_all:
                master.pasteAll();
                break;
            case R.id.clear_collapsed:
            case R.id.clear:
                master.clearClipboard();
                break;
        }
    }


    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        trace("%d", right - left);

    }

    public void onShow() {
        int visibility = clipboard().isEmpty() ? View.GONE : View.VISIBLE;
        pasteAll.setVisibility(visibility);
        pasteAllCollapsed.setVisibility(visibility);
        clear.setVisibility(visibility);
        clearCollapsed.setVisibility(visibility);
        separator1.setVisibility(visibility);
        separator2.setVisibility(visibility);
        root.requestLayout();
    }


    private void onListScroll(int dy) {
        scrollOffset -= dy;
        container.setY(scrollOffset);

        int offset = -scrollOffset;
        if (optimized && offset > container.getHeight())
            return;

        if (offset > 0) {
            collapsedContainer.setVisibility(View.VISIBLE);
//                offset += collapsedContainer.getHeight();
        } else {
            collapsedContainer.setVisibility(View.GONE);
        }

        int y = 0;
        if (offset <= y) {
            newDir.setVisibility(View.VISIBLE);
            newDir.setAlpha(1);
            newDirCollapsed.setVisibility(View.GONE);
            newDirCollapsed.setAlpha(0F);
        } else {
            newDir.setVisibility(View.VISIBLE);
            newDir.setAlpha(0);
            newDirCollapsed.setVisibility(View.VISIBLE);
            newDirCollapsed.setAlpha(1F);
        }
        applyScrollOffset(selectAll, selectAllCollapsed, offset, y);
        y += newDir.getHeight();
        y += separator1.getHeight();
        applyScrollOffset(pasteAll, pasteAllCollapsed, offset, y);
        y += selectAll.getHeight();
        applyScrollOffset(clear, clearCollapsed, offset, y);
//            y += pasteAll.getHeight();

        optimized = offset > container.getHeight();
//            container.getLayoutParams();
    }

    private void applyScrollOffset(TextView expanded, ImageView collapsed, int offset, int expandedY) {
        offset -= expandedY;
        if (offset <= 0) {
            expanded.setVisibility(View.VISIBLE);
            expanded.setAlpha(1);
            collapsed.setVisibility(View.GONE);
            collapsed.setAlpha(0F);
        } else if (offset < expanded.getHeight()) {
            expanded.setVisibility(View.VISIBLE);
            float alpha = GraphicUtils.normalizeAlpha((float) offset / expanded.getHeight());
            expanded.setAlpha(1 - alpha);
            collapsed.setVisibility(View.VISIBLE);
            collapsed.setAlpha(alpha);
        } else {
            expanded.setVisibility(View.VISIBLE);
            expanded.setAlpha(0);
            collapsed.setVisibility(View.VISIBLE);
            collapsed.setAlpha(1F);
        }
    }

    private class MyScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            onListScroll(dy);
        }

    }
}
