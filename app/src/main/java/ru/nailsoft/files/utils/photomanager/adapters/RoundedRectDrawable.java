package ru.nailsoft.files.utils.photomanager.adapters;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public abstract class RoundedRectDrawable extends MyDrawableWrapper {

    protected final Path path = new Path();

    public RoundedRectDrawable(Drawable src) {
        super(src);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();

        canvas.clipPath(path);
        src.draw(canvas);

        canvas.restore();
    }

    public static class RoundedRectDrawable1 extends RoundedRectDrawable {
        private final float rx;
        private final float ry;

        public RoundedRectDrawable1(Drawable src, float rx, float ry) {
            super(src);
            this.rx = rx;
            this.ry = ry;
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            path.reset();
            path.addRoundRect(new RectF(getBounds()), rx, ry, Path.Direction.CCW);
        }
    }

    public static class RoundedRectDrawable2 extends RoundedRectDrawable {
        private final float[] radii;

        public RoundedRectDrawable2(Drawable src, float... radii) {
            super(src);
            this.radii = radii;
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            path.reset();
            path.addRoundRect(new RectF(getBounds()), radii, Path.Direction.CCW);
        }

    }
}
