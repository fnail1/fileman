package ru.nailsoft.files.ui.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;

import ru.nailsoft.files.model.MainActivityData;
import ru.nailsoft.files.ui.main.pages.FilesFragment;


class TabsAdapter extends FragmentStatePagerAdapter {

    private final MainActivityData data;


    public TabsAdapter(FragmentManager fm, MainActivityData data) {
        super(fm);
        this.data = data;
    }

    @Override
    public Fragment getItem(int position) {
        FilesFragment fragment = new FilesFragment();

        Bundle args = new Bundle(1);
        args.putInt(FilesFragment.ARG_PAGE_INDEX, position);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public int getCount() {
        return data.tabs.size();
    }


    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return data.tabs.get(position).title;
    }

}
