package ru.nailsoft.files.ui.main;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.model.MainActivityData;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.service.ClipboardItem;
import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;
import ru.nailsoft.files.ui.BaseActivity;
import ru.nailsoft.files.ui.ReqCodes;
import ru.nailsoft.files.ui.main.pages.FilesFragment;
import ru.nailsoft.files.utils.Utils;

import static ru.nailsoft.files.App.clipboard;
import static ru.nailsoft.files.App.data;

public class MainActivity extends BaseActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        FabMenuAdapter.MasterInterface,
        MainActivityData.TabsChangeEventHandler,
        MainActivityData.TabDataChangeEventHandler,
        FilesFragment.MasterInterface,
        TabsTitlesAdapter.MasterInterface,
        MainActivityData.SelectionChangeEventHandler {

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

    private MenuItem menuCopy;
    private MenuItem menuCut;
    private MenuItem menuDelete;
    private MenuItem menuRename;
    private MenuItem menuShare;
    private MenuItem menuSearch;
    private MenuItem menuClose;

    private FabMenuAdapter fabMenuAdapter;
    private boolean fabMenuOpen = false;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(this);
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
                rebuildPath();
                tabs.getAdapter().notifyDataSetChanged();
                tabs.scrollToPosition(position);
                onSelectionChanged(data().tabs.get(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tabs.setAdapter(new TabsTitlesAdapter(this, data()));

        requestPermissions(ReqCodes.STORAGE_PERMISSION.code(), R.string.explanation_permission,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
        rebuildPath();
    }

    @Override
    protected void onPause() {
        super.onPause();
        data().tabsChanged.remove(this);
        data().tabDataChanged.remove(this);
        data().selectionChanged.remove(this);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (actionMode != ActionMode.NONE) {
            toggleActionMode(ActionMode.NONE);
            TabData tab = data().tabs.get(pages.getCurrentItem());
            tab.selection.clear();
            tab.onDataChanged();
        } else if (fabMenuOpen) {
            closeFabMenu();
        } else if (!data().tabs.get(pages.getCurrentItem()).navigateBack()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main3, menu);
        menuCopy = menu.findItem(R.id.copy);
        menuCut = menu.findItem(R.id.cut);
        menuDelete = menu.findItem(R.id.delete);
        menuRename = menu.findItem(R.id.rename);
        menuShare = menu.findItem(R.id.share);
        menuSearch = menu.findItem(R.id.search);
        menuClose = menu.findItem(R.id.close);
        actionMode = ActionMode.MANY;
        toggleActionMode(ActionMode.NONE);
        return true;
    }

    public void toggleActionMode(ActionMode mode) {
        if (mode == actionMode)
            return;
        menuCopy.setVisible(mode != ActionMode.NONE);
        menuCut.setVisible(mode != ActionMode.NONE);
        menuDelete.setVisible(mode != ActionMode.NONE);
        menuRename.setVisible(mode != ActionMode.NONE);
        menuRename.setEnabled(mode == ActionMode.SINGLE);
        menuShare.setVisible(mode != ActionMode.NONE);
        menuSearch.setVisible(mode == ActionMode.NONE);
        menuClose.setVisible(true);
        actionMode = mode;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
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
//            case R.id.search:
//                return true;
            case R.id.close:
                onCloseClick();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_camera:
                // Handle the camera action
                break;
            case R.id.nav_gallery:

                break;
            case R.id.nav_slideshow:

                break;
            case R.id.nav_manage:

                break;
            case R.id.nav_share:

                break;
            case R.id.nav_send:

                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
//        data().newTab(data().tabs.get(pages.getCurrentItem()).getPath());
        data().newTab(Environment.getExternalStorageDirectory());
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
        TabData tab = data().tabs.get(pages.getCurrentItem());
        clipboard().addAll(tab.selection, removeSource);
        tab.selection.clear();

        tab.onDataChanged();
        if (fabMenuOpen)
            buildFabMenu(true);
    }

    void onDeleteClick() {
        TabData tab = data().tabs.get(pages.getCurrentItem());
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
                    }
                    tab.selection.clear();
                    tab.onDataChanged();
                    toggleActionMode(ActionMode.NONE);
                })
                .show();
    }

    void onRenameClick() {
        View view = LayoutInflater.from(this).inflate(R.layout.dlg_rename, null);
        TabData tab = data().tabs.get(pages.getCurrentItem());
        FileItem fileItem = tab.selection.iterator().next();
        EditText text = view.findViewById(R.id.text);
        text.setText(fileItem.file.getName());

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rename_dialog_title)
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
        throw new RuntimeException("not implemented");

    }

    private void onCloseClick() {
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
        toggleActionMode(ActionMode.NONE);
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
    public void selectAll() {

    }

    @Override
    public void createNewDirectory() {

    }

    @Override
    public boolean isActionMode() {
        return actionMode != ActionMode.NONE;
    }

    @Override
    public void expandBufferedDirectoryExpand(ClipboardItem file) {

    }

    @Override
    public void closeFabMenu() {
        fabMenuOpen = false;
        fabMenu.setVisibility(View.GONE);
        fabBackground.animate()
                .alpha(0)
                .setDuration(200)
                .withEndAction(() -> {
                    fabBackground.setVisibility(View.GONE);
                });

        fab.show();
    }

    @Override
    public void pasteAll() {

    }

    @Override
    public void clearClipboard() {

    }

    private void buildFabMenu(boolean scrollToEnd) {

        ArrayList<FabMenuAdapter.Item> fabMenuItems = new ArrayList<>(7 + clipboard().size());
        fabMenuItems.add(new FabMenuAdapter.NewDirectoryItem());
        fabMenuItems.add(new FabMenuAdapter.SelectAllItem());

        if (!clipboard().isEmpty()) {
            fabMenuItems.add(new FabMenuAdapter.SeparatorItem());
            fabMenuItems.add(new FabMenuAdapter.PasteAllItem());
            fabMenuItems.add(new FabMenuAdapter.ClearItem());

            fabMenuItems.add(new FabMenuAdapter.SeparatorItem());
            for (ClipboardItem fileItem : clipboard().values()) {
                if (fileItem.file.directory)
                    fabMenuItems.add(new FabMenuAdapter.DirectoryItem(fileItem));
                else
                    fabMenuItems.add(new FabMenuAdapter.FileItem(fileItem));
            }
        }

        fabMenuItems.add(new FabMenuAdapter.SeparatorItem());
        fabMenuItems.add(new FabMenuAdapter.CloseItem());
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
        if (args != data().tabs.get(pages.getCurrentItem()))
            return;
        rebuildPath(args);

        fab.show();
    }

    private void rebuildPath() {
        rebuildPath(data().tabs.get(pages.getCurrentItem()));
    }

    private void rebuildPath(TabData args) {
        path.removeAllViews();
        File p = args.getPath();
        LayoutInflater inflater = LayoutInflater.from(this);

        do {
            TextView textView = (TextView) inflater.inflate(R.layout.item_path_element, path, false);
            textView.setText("/" + p.getName());
            textView.setOnClickListener(this::onPathItemClick);
            textView.setTag(p);
            path.addView(textView, 0);
            p = p.getParentFile();
        } while (!p.getName().isEmpty());

        pathScroll.post(() -> {
            pathScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
        });
    }

    private void onPathItemClick(View view) {
        int currentItem = pages.getCurrentItem();
        data().tabs.get(currentItem).navigate(((File) view.getTag()));
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
    public void openFile(FileItem file) {

    }

    @Override
    public void onSelectionChanged(TabData args) {
        switch (args.selection.size()) {
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

    public enum ActionMode {
        NONE, SINGLE, MANY
    }
}
