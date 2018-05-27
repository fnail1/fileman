package ru.nailsoft.files.ui.search;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.ui.ExtractDialogFragment;
import ru.nailsoft.files.ui.base.BaseActivity;
import ru.nailsoft.files.ui.main.pages.FileViewHolder;
import ru.nailsoft.files.utils.ShareHelper;
import ru.nailsoft.files.utils.Utils;

public class SearchActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    private String query;
    private File root;


    @BindView(R.id.search) SearchView search;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.appbar) AppBarLayout appbar;
    @BindView(R.id.list) RecyclerView list;
    @BindView(R.id.placeholder_text) TextView placeholderText;
    @BindView(R.id.placeholder) FrameLayout placeholder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);

        search.setOnQueryTextListener(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new MyAdapter());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);

            toolbar.setNavigationOnClickListener(this::onUpButtonClick);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.query = query;

        ThreadPool.QUICK_EXECUTORS.getExecutor(ThreadPool.Priority.MEDIUM).execute(this::performSearch);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private void performSearch() {
        String query = this.query;

        File[] files = root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().contains(query);
            }
        });

        runOnUiThread(() -> populateSearchResults(files));
    }

    private void populateSearchResults(File[] files) {

    }

    private static class MyAdapter extends RecyclerView.Adapter implements FileViewHolder.MasterInterface {

        private RecyclerView list;
        private BaseActivity context;
        private LayoutInflater inflater;
        private File[] files = new File[]{};

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            list = recyclerView;
            context = (BaseActivity) Utils.getActivity(recyclerView);
            inflater = LayoutInflater.from(context);
            super.onAttachedToRecyclerView(recyclerView);

        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            list = null;
            context = null;
            inflater = null;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(list, parent, viewType, this);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return files.length;
        }

        public File[] getFiles() {
            return files;
        }

        public void setFiles(File[] files) {
            this.files = files;
        }

        @Override
        public void openFile(FileItem file) {
            if (file.directory) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) list.getLayoutManager();
//                data.saveScrollState(layoutManager.onSaveInstanceState());
//                data.navigate(file.file);
            } else if (file.isArchive()) {
                ExtractDialogFragment.show(context, file);
            } else {
                ShareHelper.share(context, file.file.getAbsolutePath(), file.mimeType, ShareHelper.OpenMode.OPEN);
            }
        }

        @Override
        public boolean isActionMode() {
            return false;
        }

        @Override
        public boolean toggleSelection(FileItem file) {
            return false;
        }

        @Override
        public boolean isSelected(FileItem file) {
            return false;
        }
    }

    private static class MyViewHolder extends FileViewHolder {

        MyViewHolder(RecyclerView list, ViewGroup parent, int viewType, MasterInterface master) {
            super(list, parent, viewType, master);
        }
    }
}
