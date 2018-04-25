package ru.nailsoft.files.utils.photomanager.adapters;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class CircleBorderDrawer extends MyDrawableWrapper {

    private static final int[] BACKGROUNDS = {0xff0000ff, 0xff00ff00, 0xffff0000, 0xff00ffff, 0xff00ff, 0xffffff00, 0xffffffff};
    private static int backgroundIndex = 0;

    private final Paint paint;
    private final Paint fill;
    private final float borderWidth;

    public CircleBorderDrawer(Drawable src, int borderColor, float borderWidth) {
        super(src);
        this.borderWidth = borderWidth;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(borderColor);
        paint.setStrokeWidth(borderWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeCap(Paint.Cap.SQUARE);

        fill = new Paint();
        fill.setColor(BACKGROUNDS[backgroundIndex++ % BACKGROUNDS.length]);
        fill.setAntiAlias(true);
        fill.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
//        canvas.drawCircle(bounds.centerX(), bounds.centerY(), Math.min(bounds.width(), bounds.height()) / 2, fill);
        src.draw(canvas);
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), (Math.min(bounds.width(), bounds.height()) - borderWidth) / 2, paint);
    }

    @Override
    public void setAlpha(int i) {
        super.setAlpha(i);
        paint.setAlpha(i);
    }
}
