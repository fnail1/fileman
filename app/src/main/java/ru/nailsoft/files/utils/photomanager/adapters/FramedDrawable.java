package ru.nailsoft.files.utils.photomanager.adapters;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Draws selected frame of the original image to screen
 */
public class FramedDrawable extends Drawable {

    protected final Drawable src;
    protected final Rect frame;

    public FramedDrawable(Drawable src, Rect frame) {
        this.src = src;
        this.frame = frame;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        src.draw(canvas);
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
        return frame.width();
    }

    @Override
    public int getIntrinsicHeight() {
        return frame.height();
    }

    @Override
    protected void onBoundsChange(Rect screen) {
        super.onBoundsChange(screen);
        float scale = Math.min((float) screen.width() / frame.width(), (float) screen.height() / frame.height());
        float left = projectImageToScreen(frame.left, frame.width(), screen.width(), scale);
        float top = projectImageToScreen(frame.top, frame.height(), screen.height(), scale);
        float right = left + src.getIntrinsicWidth() * scale;
        float bottom = top + src.getIntrinsicHeight() * scale;
        src.setBounds((int) left, (int) top, (int) right, (int) bottom);
    }

    private float projectImageToScreen(float frameLocation, float frameDimension, float screenDimension, float scale) {
        float framePrjDimension = frameDimension * scale;
        float framePrjX = -(framePrjDimension - screenDimension) / 2;
        return -frameLocation * scale + framePrjX;
    }


}
