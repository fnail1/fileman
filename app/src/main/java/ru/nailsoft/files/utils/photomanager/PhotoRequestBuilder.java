package ru.nailsoft.files.utils.photomanager;

import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.DrawableRes;

import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.toolkit.collections.Func;
import ru.nailsoft.files.ui.ScreenMetrics;
import ru.nailsoft.files.utils.photomanager.adapters.CircleDrawable;
import ru.nailsoft.files.utils.photomanager.adapters.RoundedRectDrawable;


public class PhotoRequestBuilder<TView> {
    private final IconsManager iconsManager;
    private IconRequest.Target<TView> imageView;
    private final FileItem file;
    private ScreenMetrics.Size size;
    private int height;
    private boolean circle;
    private Func<Drawable, Drawable> extraEffect;
    boolean committed;
    private long doNotAnimateBefore;

    public PhotoRequestBuilder(IconsManager iconsManager, IconRequest.Target<TView> imageView, FileItem file) {
        this.iconsManager = iconsManager;
        this.imageView = imageView;
        this.file = file == null ? FileItem.EMPTY : file;
    }

    public PhotoRequestBuilder<TView> size(ScreenMetrics.Size size) {
        this.size = size;
        return this;
    }

    public PhotoRequestBuilder<TView> circle() {
        extraEffect = CircleDrawable::new;
        return this;
    }

    public PhotoRequestBuilder<TView> round(float rx, float ry) {
        extraEffect = (d) -> new RoundedRectDrawable.RoundedRectDrawable1(d, rx, ry);
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    public PhotoRequestBuilder<TView> round(float leftTopX, float leftTopY, float rightTopX, float rightTopY, float rightBottomX, float rightBottomY, float leftBottomX, float leftBottomY) {
        extraEffect = (d) -> new RoundedRectDrawable.RoundedRectDrawable2(d, leftTopX, leftTopY, rightTopX, rightTopY, rightBottomX, rightBottomY, leftBottomX, leftBottomY);
        return this;
    }

    public void fixTransitionConflict(int transitionDuration) {
        doNotAnimateBefore = SystemClock.elapsedRealtime() + transitionDuration;
    }

    public void commit() {
        committed = true;
        IconRequest<TView> request = new IconRequest<TView>(iconsManager, imageView, file, size, extraEffect);
        request.doNotAnimateBefore = doNotAnimateBefore;
        iconsManager.attach(request);
    }

}
