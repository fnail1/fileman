package ru.nailsoft.files.diagnostics.views;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import static ru.nailsoft.files.diagnostics.Logger.trace;


public class DbgRelativeLayout extends RelativeLayout {
    public DbgRelativeLayout(Context context) {
        super(context);
    }

    public DbgRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DbgRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DbgRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setSelected(boolean selected) {
        trace(String.valueOf(selected));
        super.setSelected(selected);
    }
}
