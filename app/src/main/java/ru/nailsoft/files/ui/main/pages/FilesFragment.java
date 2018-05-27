package ru.nailsoft.files.ui.main.pages;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.model.MainActivityData;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.service.Clipboard;
import ru.nailsoft.files.ui.base.BaseActivity;
import ru.nailsoft.files.ui.ExtractDialogFragment;
import ru.nailsoft.files.ui.base.BaseFragment;
import ru.nailsoft.files.utils.ShareHelper;

import static ru.nailsoft.files.App.clipboard;
import static ru.nailsoft.files.App.data;

public class FilesFragment extends BaseFragment implements
        MainActivityData.TabDataChangeEventHandler,
        FileViewHolder.MasterInterface,
        MainActivityData.TabsChangeEventHandler,
        MainActivityData.SelectionChangeEventHandler,
        Clipboard.ClipboardEventHandler {
    public static final String ARG_PAGE_INDEX = "page_index";

    private int index;
    private TabData data;
    private String displayedPath;

    @BindView(R.id.list) RecyclerView list;
    Unbinder unbinder;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        index = getArguments().getInt(ARG_PAGE_INDEX);
        data = data().tabs.get(index);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fr_page_navigator, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(new TabAdapter(data, this));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();
        data().tabDataChanged.add(this);
        data().tabsChanged.add(this);
        data().selectionChanged.add(this);
        clipboard().changedEvent.add(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        data().tabDataChanged.remove(this);
        data().tabsChanged.add(this);
        data().selectionChanged.remove(this);
        clipboard().changedEvent.remove(this);
    }

    @Override
    public void onTabDataChanged(MainActivityData sender, TabData args) {
        if (args != data)
            return;

        FragmentActivity activity = getActivity();
        if (activity == null)
            return;

        activity.runOnUiThread(() -> {
            list.getAdapter().notifyDataSetChanged();
            if (!data.getPath().id().equals(displayedPath)) {
                restoreScroll();
                displayedPath = data.getPath().id();
            }
        });
    }

    @Override
    public void openFile(FileItem file) {
        BaseActivity context = (BaseActivity) Objects.requireNonNull(getActivity());
        if (file.directory) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) list.getLayoutManager();
            data.saveScrollState(layoutManager.onSaveInstanceState());
            data.navigate(file.file);
        } else if (file.isArchive()) {
            ExtractDialogFragment.show(context, file);
        } else {
            ShareHelper.share(context, file.file.getAbsolutePath(), file.mimeType, ShareHelper.OpenMode.OPEN);
        }
    }

    @Override
    public boolean isActionMode() {
        return ((MasterInterface) getActivity()).isActionMode();
    }

    @Override
    public boolean toggleSelection(FileItem file) {
        return data.toggleSelection(file);
    }

    @Override
    public boolean isSelected(FileItem file) {
        return data.selection.contains(file);
    }

    @Override
    public void onTabsChanged(TabData args) {
        if (list == null)
            return;

        if (args == data) {
            restoreScroll();
        }

        if (index >= data().tabs.size())
            return;

        TabData actual = data().tabs.get(index);
        if (data != actual) {
            data = actual;
            list.setAdapter(new TabAdapter(data, this));
        }
    }

    @Override
    public void onSelectionChanged(TabData args) {
        if (args == data || args.getPath().equals(data.getPath())) {
            list.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onClipboardChanged(Clipboard.ClipboardEventArgs args) {
        list.getAdapter().notifyDataSetChanged();
    }

    private void restoreScroll() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) list.getLayoutManager();
        Parcelable state = data.scrollState();
        if (state != null) {
            layoutManager.onRestoreInstanceState(state);
        } else {
            layoutManager.scrollToPositionWithOffset(0, 0);
        }
    }

    public interface MasterInterface {

        boolean isActionMode();
    }
}
