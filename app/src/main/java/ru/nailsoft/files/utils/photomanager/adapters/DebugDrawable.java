package ru.nailsoft.files.utils.photomanager.adapters;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.util.ArrayList;


public class DebugDrawable extends MyDrawableWrapper {
    public static final int FONT_HEIGHT = 24;
    public static final int PADDING = 16;
    private final ArrayList<String> info;
    private final Paint paint;
    private final int width;
    private final int height;
    private final Paint background;

    public DebugDrawable(Drawable src) {
        super(src);
        paint = new Paint();
        paint.setTextSize(24);
        paint.setColor(0xffff0000);

        info = new ArrayList<>();
        appendInfo(src, info);

        float wmax = 0;
        for (String s : info) {
            float w = paint.measureText(s);
            if (w > wmax)
                wmax = w;
        }

        width = (int) (wmax + PADDING + 2);
        height = info.size() * FONT_HEIGHT + PADDING * 2;

        background = new Paint();
        background.setColor(0x33000000);
    }

    private void appendInfo(Drawable src, ArrayList<String> dst) {
        if (src instanceof CircleDrawable) {
            appendInfo(((CircleDrawable) src).src, dst);
            dst.add("->circle");
        } else if (src instanceof FramedDrawable) {
            FramedDrawable prev = (FramedDrawable) src;
            appendInfo(prev.src, dst);
            dst.add("->frame " + prev.frame);
        } else if (src instanceof RoundedRectDrawable) {
            appendInfo(((RoundedRectDrawable) src).src, dst);
            dst.add("->rounded");
        } else if (src instanceof AutoScaledDrawable) {
            AutoScaledDrawable prev = ((AutoScaledDrawable) src);
            appendInfo(prev.src, dst);
            dst.add("->project " + prev.rect.left + ", " + prev.rect.top + " - " + prev.rect.right + ", " + prev.rect.bottom);
        } else if (src == null) {
            dst.add("->null");
        } else {
            dst.add("->" + src.getIntrinsicWidth() + "x" + src.getIntrinsicHeight());
        }
    }

    @Override
    public void setAlpha(int i) {
        super.setAlpha(i);
        paint.setAlpha(i);
        background.setAlpha(i * 0x33 / 0x100);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        src.draw(canvas);
        canvas.drawRect(0, 0, width, height, background);
        for (int i = 0; i < info.size(); i++) {
            String s = info.get(i);
            canvas.drawText(s, PADDING, FONT_HEIGHT * (i + 1) + PADDING, paint);
        }

    }
}
