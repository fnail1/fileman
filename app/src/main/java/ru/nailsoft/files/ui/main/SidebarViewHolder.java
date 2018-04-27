package ru.nailsoft.files.ui.main;

import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.nailsoft.files.R;

public class SidebarViewHolder implements NavigationView.OnNavigationItemSelectedListener, DrawerLayout.DrawerListener {
    private final MainActivity master;
    private final DrawerLayout drawerLayout;
    private final NavigationView root;
    private final MenuItem orderCreate;
    private final MenuItem orderModified;
    private final MenuItem orderSize;
    @BindView(R.id.image) ImageView imageView;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.subtitle) TextView subtitle;
    private final MenuItem orderName;


    public SidebarViewHolder(MainActivity master, DrawerLayout drawerLayout, Toolbar toolbar, NavigationView root) {
        this.master = master;
        this.drawerLayout = drawerLayout;
        this.root = root;
        ButterKnife.bind(this, root.getHeaderView(0));
        Menu menu = root.getMenu();
        orderName = menu.findItem(R.id.order_name);
        orderCreate = menu.findItem(R.id.order_create);
        orderModified = menu.findItem(R.id.order_modified);
        orderSize = menu.findItem(R.id.order_size);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                master, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        toggle.syncState();

        root.setNavigationItemSelectedListener(this);
        drawerLayout.addDrawerListener(toggle);
        drawerLayout.addDrawerListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
// Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_camera:
                // Handle the camera action
                break;
            case R.id.nav_share:

                break;
            case R.id.nav_send:

                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
