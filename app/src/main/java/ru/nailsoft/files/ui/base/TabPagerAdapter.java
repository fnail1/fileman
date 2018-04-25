package ru.nailsoft.files.ui.base;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.ArrayList;

public class TabPagerAdapter extends FragmentStatePagerAdapter {

    private final Context mContext;
    private final ArrayList<TabInfo> mTabs = new ArrayList<>();
    private SparseArray<Fragment> registeredFragments = new SparseArray<>();

    public TabPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    public void addTab(String title, Class<?> clss, Bundle args) {
        TabInfo info = new TabInfo(title, clss, args);
        mTabs.add(info);
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int index) {
        TabInfo info = mTabs.get(index);
        return Fragment.instantiate(mContext, info.clss.getName(), info.args);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabs.get(position).title;
    }

    public Fragment getFragment(int index) {
        return registeredFragments.get(index);
    }

    private class TabInfo {
        private String title;
        private Class<?> clss;
        private Bundle args;

        TabInfo(String title, Class<?> clss, Bundle args) {
            this.title = title;
            this.clss = clss;
            this.args = args;
        }
    }
}
