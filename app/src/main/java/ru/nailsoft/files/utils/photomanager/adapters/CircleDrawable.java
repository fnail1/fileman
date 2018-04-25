package ru.nailsoft.files.utils.photomanager.adapters;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class CircleDrawable extends MyDrawableWrapper {

    private final Path path;

    public CircleDrawable(Drawable src) {
        super(src);

        int cX = src.getIntrinsicWidth() / 2;
        int cY = src.getIntrinsicHeight() / 2;
        path = new Path();
        path.addCircle(cX, cY, Math.min(cX, cY), Path.Direction.CCW);

    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.clipPath(path);
        src.draw(canvas);
        canvas.restore();
    }


    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        path.reset();
        int cX = (right + left) / 2;
        int cY = (bottom + top) / 2;
        path.addCircle(cX, cY, Math.min(cX, cY), Path.Direction.CCW);
    }

    @Override
    public void setBounds(@NonNull Rect bounds) {
        super.setBounds(bounds);
        path.reset();
        int cX = bounds.centerX();
        int cY = bounds.centerY();
        path.addCircle(cX, cY, Math.min(cX, cY), Path.Direction.CCW);
    }

}
