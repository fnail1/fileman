package ru.nailsoft.files.utils.photomanager.adapters;

import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class MyDrawableWrapper extends Drawable {
    protected final Drawable src;

    public MyDrawableWrapper(Drawable src) {
        this.src = src;
    }

    @Override
    public void setAlpha(int i) {
        src.setAlpha(i);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        src.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return src.getOpacity();
    }

    @Override
    public int getIntrinsicWidth() {
        return src.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return src.getIntrinsicHeight();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        src.setBounds(left, top, right, bottom);
        super.setBounds(left, top, right, bottom);
    }
}
