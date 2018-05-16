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
import ru.nailsoft.files.model.TabData;

public class SidebarViewHolder implements NavigationView.OnNavigationItemSelectedListener, DrawerLayout.DrawerListener {
    private final MainActivity master;
    private final DrawerLayout drawerLayout;
    private final NavigationView root;
    private final MenuItem orderModified;
    private final MenuItem orderSize;
    @BindView(R.id.image) ImageView imageView;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.subtitle) TextView subtitle;
    private final MenuItem orderName;


    SidebarViewHolder(MainActivity master, DrawerLayout drawerLayout, Toolbar toolbar, NavigationView root) {
        this.master = master;
        this.drawerLayout = drawerLayout;
        this.root = root;
        ButterKnife.bind(this, root.getHeaderView(0));
        Menu menu = root.getMenu();
        orderName = menu.findItem(R.id.order_name);
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
        int id = item.getItemId();

        switch (id) {
            case R.id.order_name:
                setOrderCriteria(TabData.Order.NAME_ASC, TabData.Order.NAME_DESC);
                break;
            case R.id.order_size:
                setOrderCriteria(TabData.Order.SIZE_ASC, TabData.Order.SIZE_DESC);
                break;
            case R.id.order_modified:
                setOrderCriteria(TabData.Order.MOFIFIED_ASC, TabData.Order.MOFIFIED_DESC);
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setOrderCriteria(TabData.Order asc, TabData.Order desc) {
        setOrderCriteria(master.currentTab().getOrder() == asc ? desc : asc);
    }

    private void setOrderCriteria(TabData.Order order) {
        master.currentTab().setOrder(order);
        updateOrderIcon(order);
    }

    private void updateOrderIcon(TabData.Order order) {
        switch (order) {
            case NAME_ASC:
                orderName.setIcon(R.drawable.ic_sort_asc);
                break;
            case NAME_DESC:
                orderName.setIcon(R.drawable.ic_sort_desc);
                break;
            default:
                orderName.setIcon(null);
                break;
        }

        switch (order) {
            case MOFIFIED_ASC:
                orderModified.setIcon(R.drawable.ic_sort_asc);
                break;
            case MOFIFIED_DESC:
                orderModified.setIcon(R.drawable.ic_sort_desc);
                break;
            default:
                orderModified.setIcon(null);
                break;
        }

        switch (order) {
            case SIZE_ASC:
                orderSize.setIcon(R.drawable.ic_sort_asc);
                break;
            case SIZE_DESC:
                orderSize.setIcon(R.drawable.ic_sort_desc);
                break;
            default:
                orderSize.setIcon(null);
                break;
        }
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

    public void onCurrentTabChanged(TabData tab) {
        updateOrderIcon(tab.getOrder());
    }
}
