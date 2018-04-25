package ru.nailsoft.files;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
    private static final String COMPLIMENTS_BONUSES_DELIMITER = ";";

    private static final String VERSION = "version";
    private static final String STORED_UNIQUE_ID = "STORED_UNIQUE_ID";
    private static final String PREFIX_PERMISSION_REQUESTED = "permission:";


    private final SharedPreferences common;
    private int oldVersion;

    Preferences(App context) {
        common = PreferenceManager.getDefaultSharedPreferences(context);
        onOpen(context);
    }

    private void onOpen(Context context) {
        oldVersion = common.getInt(VERSION, 1);

        if (oldVersion != BuildConfig.VERSION_CODE) {

            SharedPreferences.Editor commonEditor = common.edit();
            commonEditor.putInt(VERSION, BuildConfig.VERSION_CODE).apply();
        }
    }

    public int getOldVersion() {
        return oldVersion;
    }

    public boolean isPermissionRequested(String permission) {
        return common.getBoolean(PREFIX_PERMISSION_REQUESTED + permission, false);
    }

    public void onPermissionRequested(String permission) {
        common.edit().putBoolean(PREFIX_PERMISSION_REQUESTED + permission, true).apply();
    }


}
