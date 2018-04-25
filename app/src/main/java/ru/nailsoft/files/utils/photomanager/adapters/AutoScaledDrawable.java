package ru.nailsoft.files.utils.photomanager.adapters;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import ru.nailsoft.files.utils.GraphicUtils;

public abstract class AutoScaledDrawable extends MyDrawableWrapper {

    private final int width;
    private final int height;
    protected final Rect rect;

    public AutoScaledDrawable(@NonNull Drawable src, int width, int height, Rect projection) {
        super(src);
        this.width = width;
        this.height = height;
        rect = projection;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        src.setBounds(rect);
        src.draw(canvas);
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    public static class In extends AutoScaledDrawable {
        public In(@NonNull Drawable src, int width, int height) {
            super(src, width, height, GraphicUtils.projectIn(
                    src.getIntrinsicWidth(), src.getIntrinsicHeight(),
                    width, height));
        }
    }

    public static class Out extends AutoScaledDrawable {
        public Out(@NonNull Drawable src, int width, int height) {
            super(src, width, height, GraphicUtils.projectOut(
                    src.getIntrinsicWidth(), src.getIntrinsicHeight(),
                    width, height));
        }
    }
}
