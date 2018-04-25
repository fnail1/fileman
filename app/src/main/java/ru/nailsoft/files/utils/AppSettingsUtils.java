package ru.nailsoft.files.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class AppSettingsUtils {
    /**
     * Opens application settings.
     *
     * @param context Context.
     */
    public static void openAppSettings(Context context) {
        final Intent goToAppPreferencesIntent = new Intent();
        goToAppPreferencesIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        goToAppPreferencesIntent.addCategory(Intent.CATEGORY_DEFAULT);
        goToAppPreferencesIntent.setData(Uri.parse("package:" + context.getPackageName()));
        goToAppPreferencesIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        goToAppPreferencesIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        goToAppPreferencesIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(goToAppPreferencesIntent);
    }
}
