package ru.nailsoft.files.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.model.MainActivityData;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.service.Clipboard;
import ru.nailsoft.files.service.ClipboardItem;
import ru.nailsoft.files.service.CopyService;
import ru.nailsoft.files.service.CopyTask;
import ru.nailsoft.files.service.ZipTask;
import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.toolkit.concurrent.ExclusiveExecutor2;
import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;
import ru.nailsoft.files.ui.CopyDialogFragment;
import ru.nailsoft.files.ui.OpenAsDialog;
import ru.nailsoft.files.ui.ReqCodes;
import ru.nailsoft.files.ui.base.BaseActivity;
import ru.nailsoft.files.ui.main.pages.FilesFragment;
import ru.nailsoft.files.utils.ShareHelper;
import ru.nailsoft.files.utils.Utils;

import static ru.nailsoft.files.App.clipboard;
import static ru.nailsoft.files.App.copy;
import static ru.nailsoft.files.App.data;

public class MainActivity extends BaseActivity
        implements
        FabMenuAdapter.MasterInterface,
        MainActivityData.TabsChangeEventHandler,
        MainActivityData.TabDataChangeEventHandler,
        FilesFragment.MasterInterface,
        TabsTitlesAdapter.MasterInterface,
        MainActivityData.SelectionChangeEventHandler,
        Clipboard.ClipboardEventHandler, SearchView.OnQueryTextListener {

    @BindView(R.id.path) LinearLayout path;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.pages) ViewPager pages;
    @BindView(R.id.tabs) RecyclerView tabs;
    @BindView(R.id.fab_menu) RecyclerView fabMenu;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.nav_view) NavigationView navView;
    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
    @BindView(R.id.path_scroll) HorizontalScrollView pathScroll;
    @BindView(R.id.new_tab) ImageView newTab;
    @BindView(R.id.fab_background) View fabBackground;

    private MenuItem menuOpen;
    private MenuItem menuCopy;
    private MenuItem menuCut;
    private MenuItem menuDelete;
    private MenuItem menuRename;
    private MenuItem menuShare;
    private MenuItem menuSearch;
    private MenuItem menuZip;
    private MenuItem menuClose;

    private FabMenuAdapter fabMenuAdapter;
    private boolean fabMenuOpen = false;
    private ActionMode actionMode;
    private SidebarViewHolder sidebarViewHolder;
    private ExclusiveExecutor2 filterExecutor = new ExclusiveExecutor2(0, ThreadPool.SCHEDULER, this::onFilterChanged);
    private String filter;
    private SearchView searchView;
    private TabData.AbsHistoryItem searchRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);


        fabMenuAdapter = new FabMenuAdapter(this);
        fabMenu.setLayoutManager(new LinearLayoutManager(this));
        fabMenu.setAdapter(fabMenuAdapter);

        pages.setAdapter(new TabsAdapter(getSupportFragmentManager(), data()));
        pages.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                onCurrentTabChanged(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tabs.setAdapter(new TabsTitlesAdapter(this, data()));

        sidebarViewHolder = new SidebarViewHolder(this, drawerLayout, toolbar, navView);

        requestPermissions(ReqCodes.STORAGE_PERMISSION.code(), R.string.explanation_permission,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void onCurrentTabChanged(int position) {
        TabData tab = data().tabs.get(position);

        rebuildPath(tab);
        tabs.getAdapter().notifyDataSetChanged();
        tabs.scrollToPosition(position);
        onSelectionChanged(tab);
        sidebarViewHolder.onCurrentTabChanged(tab);
        restoreSearchView(tab);
    }

    private void restoreSearchView(TabData tab) {
        if (searchView != null) {
            TabData.AbsHistoryItem historyItem = tab.getPath();
            String filter = historyItem.getFilter();
            searchView.setQuery(filter, false);
            searchView.setIconified(TextUtils.isEmpty(filter) && !(historyItem instanceof TabData.SearchHistoryItem));
        }
    }

    @Override
    protected void onRequestedPermissionsGranted(int requestCode, String[] permissions, int[] grantResults) {
        if (ReqCodes.byCode(requestCode) == ReqCodes.STORAGE_PERMISSION) {
            data().onPermissionGranted();
            return;
        }
        super.onRequestedPermissionsGranted(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        data().tabsChanged.add(this);
        data().tabDataChanged.add(this);
        data().selectionChanged.add(this);
        clipboard().changedEvent.add(this);
        rebuildPath();
        sidebarViewHolder.onCurrentTabChanged(currentTab());
    }

    @Override
    protected void onPause() {
        super.onPause();
        data().tabsChanged.remove(this);
        data().tabDataChanged.remove(this);
        data().selectionChanged.remove(this);
        clipboard().changedEvent.remove(this);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        TabData tab = currentTab();
        if (actionMode != ActionMode.NONE) {
            toggleActionMode(ActionMode.NONE);
            tab.selection.clear();
            tab.onDataChanged();
            return;
        }

        if (fabMenuOpen) {
            closeFabMenu();
            return;
        }

        if (!searchView.isIconified() && (searchRoot == null || searchRoot.same(tab.getPath()))) {
            searchView.setIconified(true);
            return;
        }

        if (tab.navigateBack()) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main3, menu);
        menuOpen = menu.findItem(R.id.open);
        menuCopy = menu.findItem(R.id.copy);
        menuCut = menu.findItem(R.id.cut);
        menuDelete = menu.findItem(R.id.delete);
        menuRename = menu.findItem(R.id.rename);
        menuShare = menu.findItem(R.id.share);
        menuSearch = menu.findItem(R.id.search);
        searchView = ((SearchView) menuSearch.getActionView());
        searchView.setOnQueryTextListener(this);
        menuZip = menu.findItem(R.id.zip);
        menuClose = menu.findItem(R.id.close);
        actionMode = ActionMode.MANY;
        onSelectionChanged(currentTab());
        return true;
    }

    public void toggleActionMode(ActionMode mode) {
        if (mode == actionMode)
            return;
        if (menuOpen == null)
            return;
        menuOpen.setVisible(mode != ActionMode.NONE);
        menuOpen.setEnabled(mode == ActionMode.SINGLE);
        menuCopy.setVisible(mode != ActionMode.NONE);
        menuCut.setVisible(mode != ActionMode.NONE);
        menuDelete.setVisible(mode != ActionMode.NONE);
        menuRename.setVisible(mode != ActionMode.NONE);
        menuRename.setEnabled(mode == ActionMode.SINGLE);
        menuShare.setVisible(mode != ActionMode.NONE);
        menuSearch.setVisible(mode == ActionMode.NONE);
        menuZip.setVisible(mode != ActionMode.NONE);
        menuClose.setVisible(true);
        path.setVisibility(mode == ActionMode.NONE ? View.VISIBLE : View.GONE);
        actionMode = mode;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.open:
                onOpenAsClick();
                return true;
            case R.id.copy:
                onCopyClick();
                return true;
            case R.id.cut:
                onCutClick();
                return true;
            case R.id.delete:
                onDeleteClick();
                return true;
            case R.id.rename:
                onRenameClick();
                return true;
            case R.id.share:
                onShareClick();
                return true;
            case R.id.zip:
                onZipClick();
                return true;
            case R.id.close:
                onCloseClick();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.fab)
    void onFabClick() {
        if (fabMenuOpen)
            closeFabMenu();
        else
            openFabMenu();
    }

    @OnClick(R.id.new_tab)
    public void onNewTabClick() {
//        data().newTab(currentTab().getPath());
        data().newTab().navigate(Environment.getExternalStorageDirectory());
    }

    private void onOpenAsClick() {
        TabData tab = currentTab();

        if (tab.selection.size() != 1)
            return;
        FileItem fileItem = tab.selection.iterator().next();

        tab.selection.clear();
        tab.onDataChanged();
        toggleActionMode(ActionMode.NONE);

        OpenAsDialog.show(this, fileItem);
    }

    void onCopyClick() {
        addSelectionToClipboard(false);
        toggleActionMode(ActionMode.NONE);
    }

    void onCutClick() {
        addSelectionToClipboard(true);
        toggleActionMode(ActionMode.NONE);
    }

    private void addSelectionToClipboard(boolean removeSource) {
        TabData tab = currentTab();
        clipboard().addAll(tab.selection, removeSource);
        tab.selection.clear();

        tab.onDataChanged();
    }

    void onDeleteClick() {
        TabData tab = currentTab();
        String message;
        switch (tab.selection.size()) {
            case 0:
                return;
            case 1:
                FileItem file = tab.selection.iterator().next();
                if (file.directory)
                    message = getString(R.string.delete_single_dir_message, file.file.getName());
                else
                    message = getString(R.string.delete_single_file_message, file.file.getName());
                break;
            default:
                message = getString(R.string.delete_format_many_message, tab.selection.size());
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    for (FileItem fileItem : tab.selection) {
                        FileUtils.deleteRecursive(fileItem.file);
                        tab.onDelete(fileItem);
                    }
                    tab.selection.clear();
                    tab.onDataChanged();
                    toggleActionMode(ActionMode.NONE);
                })
                .show();
    }

    void onRenameClick() {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null);
        TabData tab = currentTab();
        FileItem fileItem = tab.selection.iterator().next();
        EditText text = view.findViewById(R.id.text);
        text.setText(fileItem.file.getName());

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rename)
                .setView(view)
                .setPositiveButton(R.string.rename, (dialog, which) -> {
                    try {
                        File newFile = new File(fileItem.file.getParentFile(), text.getText().toString());
                        FileUtils.rename(fileItem.file, newFile);
                        tab.selection.clear();
                        tab.onRename(fileItem, newFile);
                        toggleActionMode(ActionMode.NONE);
                    } catch (IOException | FileOpException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .create();
        alertDialog.setOnShowListener(dialog -> Utils.showKeyboard(text));

        alertDialog.show();

    }

    void onShareClick() {
        TabData tab = currentTab();
        ArrayList<FileItem> files = new ArrayList<>(tab.selection);
        ShareHelper.share(this, files);
        tab.selection.clear();
        tab.onDataChanged();
        toggleActionMode(ActionMode.NONE);
    }

    private void onZipClick() {

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null);
        TabData tab = currentTab();
        ArrayList<FileItem> items = new ArrayList<>(tab.selection);
        EditText text = view.findViewById(R.id.text);
        String n = items.size() == 1 ? items.get(0).name : tab.getPath().title();
        n = FileUtils.replaceExt(n, "zip");
        text.setText(n);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.compress_files)
                .setView(view)
                .setPositiveButton(R.string.compress, (dialog, which) -> {
                    ZipTask task = new ZipTask(items, tab, text.getText().toString());
                    copy().enqueue(task);
                    CopyDialogFragment.show(this);
                    tab.selection.clear();
                    tab.onDataChanged();
                    toggleActionMode(ActionMode.NONE);
                })
                .create();
        alertDialog.setOnShowListener(dialog -> Utils.showKeyboard(text));

        alertDialog.show();

    }

    private void onCloseClick() {
        if (actionMode != ActionMode.NONE) {
            toggleActionMode(ActionMode.NONE);
            TabData tab = currentTab();
            tab.selection.clear();
            tab.onDataChanged();
        } else if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            int currentItem = pages.getCurrentItem();
            if (currentItem == 0) {
                if (data().tabs.size() == 1) {
                    finish();
                } else {
                    data().closeTab(data().tabs.get(currentItem));
                }
            } else {
                pages.setCurrentItem(currentItem - 1);
                data().closeTab(data().tabs.get(currentItem));
            }
        }
    }

    private void openFabMenu() {
        buildFabMenu(false);

        fabMenuOpen = true;

        fabMenu.setScaleX(0);
        fabMenu.setScaleY(0);
        fabMenu.setVisibility(View.VISIBLE);
        fabMenu.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(200);

        fabBackground.setVisibility(View.VISIBLE);
        fabBackground.setAlpha(0);
        fabBackground.animate()
                .alpha(1)
                .setDuration(200);

        fab.hide();
    }

    @Override
    public void closeFabMenu() {
        fabMenuOpen = false;
        fabMenu.setVisibility(View.GONE);
        fabBackground.animate()
                .alpha(0)
                .setDuration(200)
                .withEndAction(() -> fabBackground.setVisibility(View.GONE));

        fab.show();
    }

    @Override
    public void onClipboardChanged(Clipboard.ClipboardEventArgs args) {
        if (fabMenuOpen) {
            buildFabMenu(args.hasNew);
        } else {
            Animation selectionChangedAnimation = new Animation() {
                @Override
                protected void applyTransformation(float alpha, Transformation t) {
                    super.applyTransformation(alpha, t);
                    float scale = (float) (1 + 0.2 * (1 - 2 * Math.abs(alpha - 0.5)));
                    fab.setScaleX(scale);
                    fab.setScaleY(scale);
                }
            };
            selectionChangedAnimation.setDuration(400);
            fab.startAnimation(selectionChangedAnimation);
        }
    }

    @Override
    public void selectAll() {
        currentTab().selectAll();
        closeFabMenu();
    }

    @Override
    public void createNewDirectory() {
        NewDirectoryDialog.show(this, currentTab());
        closeFabMenu();
    }

    @Override
    public boolean isActionMode() {
        return actionMode != ActionMode.NONE;
    }

    @Override
    public void expandBufferedDirectoryExpand(ClipboardItem file) {

    }

    @Override
    public void pasteAll() {
        CopyTask task = new CopyTask(clipboard().values(), currentTab());
        copy().enqueue(task);
        clipboard().clear();
        CopyDialogFragment.show(this);
        closeFabMenu();
    }

    @Override
    public void paste(ClipboardItem file) {
        CopyTask task = new CopyTask(Collections.singleton(file), currentTab());
        copy().enqueue(task);
        clipboard().remove(file);
        CopyDialogFragment.show(this);
        closeFabMenu();

    }

    @Override
    public void clearClipboard() {
        clipboard().clear();
        closeFabMenu();
    }

    private void buildFabMenu(boolean scrollToEnd) {

        ArrayList<FabMenuAdapter.Item> fabMenuItems = new ArrayList<>(7 + clipboard().size());
        fabMenuItems.add(new FabMenuAdapter.NewDirectoryItem(this));
        fabMenuItems.add(new FabMenuAdapter.SelectAllItem());

        if (!clipboard().isEmpty()) {
            fabMenuItems.add(new FabMenuAdapter.SeparatorItem());
            fabMenuItems.add(new FabMenuAdapter.PasteAllItem(this));
            fabMenuItems.add(new FabMenuAdapter.ClearItem(this));

            fabMenuItems.add(new FabMenuAdapter.SeparatorItem());
            for (ClipboardItem fileItem : clipboard().values()) {
                if (fileItem.file.directory)
                    fabMenuItems.add(new FabMenuAdapter.DirectoryItem(fileItem));
                else
                    fabMenuItems.add(new FabMenuAdapter.FileItem(fileItem));
            }
        }

//        fabMenuItems.add(new FabMenuAdapter.SeparatorItem());
//        fabMenuItems.add(new FabMenuAdapter.CloseItem(this));
        fabMenuAdapter.setItems(fabMenuItems);

        if (scrollToEnd)
            fabMenu.scrollToPosition(fabMenuAdapter.getItemCount() - 1);
    }

    @Override
    public void onTabsChanged(TabData args) {
        pages.getAdapter().notifyDataSetChanged();
        tabs.getAdapter().notifyDataSetChanged();
        if (args != null) {
            pages.setCurrentItem(data().tabs.indexOf(args));
            onSelectionChanged(args);
        }

    }

    @Override
    public void onTabDataChanged(MainActivityData sender, TabData args) {
        if (args != currentTab())
            return;
        runOnUiThread(() -> {
            rebuildPath(args);
            restoreSearchView(args);
            tabs.getAdapter().notifyItemChanged(data().tabs.indexOf(args));
            fab.show();
        });
    }

    private void rebuildPath() {
        rebuildPath(currentTab());
    }

    private void rebuildPath(TabData args) {
        path.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        TabData.AbsHistoryItem historyItem = args.getPath();
        if (historyItem instanceof TabData.DirectoryHistoryItem) {
            File p = ((TabData.DirectoryHistoryItem) historyItem).path;

            do {
                TextView textView = (TextView) inflater.inflate(R.layout.item_path_element, this.path, false);
                textView.setText("/" + p.getName());
                textView.setOnClickListener(this::onPathItemClick);
                textView.setTag(p);
                this.path.addView(textView, 0);
                p = p.getParentFile();
            } while (!p.getName().isEmpty());
        } else if (historyItem instanceof TabData.SearchHistoryItem) {
            TabData.SearchHistoryItem searchHistoryItem = (TabData.SearchHistoryItem) historyItem;
            TextView textView = (TextView) inflater.inflate(R.layout.item_path_element, this.path, false);
            textView.setText(searchHistoryItem.title());
            this.path.addView(textView, 0);
        }

        pathScroll.post(() -> pathScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private void onPathItemClick(View view) {
        File path = (File) view.getTag();
        if (path == null)
            return;
        currentTab().navigate(path);
    }

    @Override
    public void onTabClick(TabData tab) {
        pages.setCurrentItem(data().tabs.indexOf(tab));
    }

    @Override
    public TabData currentTab() {
        return data().tabs.get(pages.getCurrentItem());
    }

    @Override
    public void onSelectionChanged(TabData args) {
        int selectionSize = args.selection.size();
        switch (selectionSize) {
            case 0:
                toggleActionMode(ActionMode.NONE);
                break;
            case 1:
                toggleActionMode(ActionMode.SINGLE);
                break;
            default:
                toggleActionMode(ActionMode.MANY);
                break;
        }
    }

    @OnClick(R.id.fab_background)
    public void onFabMenuBackgroundClick() {
        closeFabMenu();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filter = newText;
        searchRoot = currentTab().getPath();
        filterExecutor.execute(false);
        return false;
    }

    private void onFilterChanged() {
        currentTab().setFilter(filter);
    }

    public enum ActionMode {
        NONE, SINGLE, MANY
    }
}
