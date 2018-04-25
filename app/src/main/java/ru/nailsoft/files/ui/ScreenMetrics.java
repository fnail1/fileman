package ru.nailsoft.files.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import ru.nailsoft.files.R;

import static ru.nailsoft.files.App.app;

public class ScreenMetrics {

    public final Size screen;
    public final Size icon;
    public final Size notificationIcon;
    public final int indent;
    public final int iconRoundRadius;
    public final Size menuIcon;

    public ScreenMetrics(Context context) {
        Resources resources = context.getResources();

        indent = resources.getDimensionPixelOffset(R.dimen.padding_half);

        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        screen = new Size(displayMetrics.widthPixels, displayMetrics.heightPixels);

        int iconSizePixels = resources.getDimensionPixelSize(R.dimen.icon_size);
        icon = new Size(iconSizePixels, iconSizePixels);

        int notificationIconSize = resources.getDimensionPixelSize(R.dimen.notification_image_size);
        notificationIcon = new Size(notificationIconSize, notificationIconSize);

        iconRoundRadius = indent;

        int menuIconSize = resources.getDimensionPixelSize(R.dimen.menu_icon_size);
        menuIcon = new Size(menuIconSize, menuIconSize);
    }

    public static boolean isPortrait() {
        return app().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }


    public static final class Size {
        public final int width;
        public final int height;

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
