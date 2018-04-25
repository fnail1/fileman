package ru.nailsoft.files.utils.photomanager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ru.nailsoft.files.diagnostics.Logger;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.toolkit.collections.Func;
import ru.nailsoft.files.ui.ScreenMetrics;
import ru.nailsoft.files.utils.GraphicUtils;
import ru.nailsoft.files.utils.photomanager.adapters.AutoScaledDrawable;
import ru.nailsoft.files.utils.photomanager.adapters.DebugDrawable;

import static ru.nailsoft.files.App.app;
import static ru.nailsoft.files.App.screenMetrics;
import static ru.nailsoft.files.diagnostics.Logger.logV;

public final class IconRequest<TView> implements Runnable {
    private final static AtomicInteger counter = new AtomicInteger();
    private final static HashSet<String> history = new HashSet<>();

    public final Target<TView> viewHolder;
    private final Func<Drawable, Drawable> extraEffect;
    private final ScreenMetrics.Size size;
    private FileItem file;
    private final IconsManager iconsManager;
    private Drawable icon;
    private final long startTs;
    private final int name;
    public long doNotAnimateBefore;
    private String cacheKey;

    public IconRequest(IconsManager pm,
                       @Nullable Target<TView> viewHolder,
                       @NonNull FileItem file,
                       ScreenMetrics.Size size,
                       @Nullable Func<Drawable, Drawable> extraEffect) {
        startTs = SystemClock.elapsedRealtime();
        name = counter.incrementAndGet();

        iconsManager = pm;
        this.file = file;
        this.size = size;
        this.extraEffect = extraEffect;
        this.viewHolder = viewHolder;
        cacheKey = file.file.getAbsolutePath();
    }


    private boolean checkContext() {
        TView imageView = viewHolder.viewRef.get();
        return imageView != null && file.equals(viewHolder.getTag(imageView));
    }

    boolean bind() {
        TView imageView = viewHolder.viewRef.get();
        if (imageView == null || file.equals(viewHolder.getTag(imageView)))
            return false;

        viewHolder.setTag(imageView, file);
        return true;
    }

    @UiThread
    public void start() {
        log("start");

        if (file.directory) {
            apply();
            return;
        }

        Bitmap bitmap = iconsManager.cache.get(cacheKey);
        boolean complete = bitmap != null;

        if (complete && !file.ext.isEmpty())
            bitmap = iconsManager.cache.get(file.ext);

        if (bitmap != null) {
            icon = new BitmapDrawable(app().getResources(), bitmap);
        }
        apply();

        if (!complete) {
            ThreadPool.QUICK_EXECUTORS.getExecutor(ThreadPool.Priority.LOWEST).execute(this);
        }
    }

    @Override
    public void run() {
        if (!checkContext()) {
            log("cancel");
            return;
        }
        if (ThreadPool.isUiThread()) {
            log("UI");
            apply();
            return;
        }

        if (file.mimeType != null && icon == null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = DocumentsContract.buildDocumentUri(app().getApplicationContext().getPackageName() + ".provider", file.file.getAbsolutePath());
            intent.setData(uri);
            intent.setType(file.mimeType);

            PackageManager packageManager = app().getPackageManager();
            List<ResolveInfo> matches = packageManager.queryIntentActivities(intent, 0);
            if (!matches.isEmpty()) {
                icon = matches.get(0).loadIcon(packageManager);
            }
            if (icon != null) {
                ThreadPool.UI.post(this);
            }
        }

        if (icon == null) {
            Bitmap bitmap = iconsManager.generateForExtension(file.ext);
            iconsManager.cache.update(file.ext, bitmap);
            icon = new BitmapDrawable(app().getResources(), bitmap);
            ThreadPool.UI.post(this);
        }

        if (file.mimeType != null && file.mimeType.startsWith("image/")) {
            try {
                Bitmap bitmap = GraphicUtils.decodeUri(file.file.getAbsolutePath(), screenMetrics().icon.width, screenMetrics().icon.height);
                if (bitmap != null) {
                    iconsManager.cache.update(cacheKey, bitmap);
                    icon = new BitmapDrawable(app().getResources(), bitmap);
                    ThreadPool.UI.post(this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @UiThread
    private void apply() {
        boolean animate = SystemClock.elapsedRealtime() - startTs > 500;

        TView imageView = viewHolder.viewRef.get();
        if (imageView == null)
            return;

        if (icon == null)
            return;

        Drawable d = icon;

        d = viewHolder.staticEffect(this, imageView, d);

        if (extraEffect != null) {
            d = extraEffect.invoke(d);
        }

        if (Logger.LOG_GLIDE) {
            d = new DebugDrawable(d);
        }


        viewHolder.apply(this, imageView, d, animate);
    }

    private void log(String message) {
        logV(Logger.LOG_GLIDE, Logger.TAG_GLIDE, "%d %s %s %s (%dx%d)",
                name, message, cacheKey,
                file.name,
                size.width, size.height);
    }

public abstract static class AbstractPlaceholder<TView> {

    static AbstractPlaceholder wrap(@DrawableRes int resId) {
        return new ResourcePlaceholder(resId);
    }

    static AbstractPlaceholder wrap(Drawable drawable) {
        return new DrawablePlaceholder(drawable);
    }

    protected AbstractPlaceholder() {
    }

    protected abstract void apply(IconRequest<TView> request);

    protected void apply(IconRequest<TView> request, TView imageView, Drawable drawable) {
        Drawable d = request.viewHolder.staticEffect(request, imageView, drawable);

        if (Logger.LOG_GLIDE) {
            d = new DebugDrawable(d);
        }

        request.viewHolder.apply(request, imageView, d, false);
    }


}

private static class ResourcePlaceholder<TView extends View> extends AbstractPlaceholder<TView> {
    private final int resId;

    ResourcePlaceholder(@DrawableRes int resId) {
        this.resId = resId;
    }

    @Override
    protected void apply(IconRequest<TView> request) {
        TView imageView = request.viewHolder.viewRef.get();
        if (imageView == null)
            return;

        Context context = imageView.getContext();
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), resId, context.getTheme());
        if (drawable != null) {
//                    drawable = new AutoScaledDrawable(drawable, request.targetWidth, request.targetHeight);
            apply(request, imageView, drawable);
        }
    }

}

private static class DrawablePlaceholder<TView extends View> extends AbstractPlaceholder<TView> {
    private final Drawable drawable;

    DrawablePlaceholder(Drawable drawable) {
        this.drawable = drawable;
    }

    @Override
    protected void apply(IconRequest<TView> request) {
        TView imageView = request.viewHolder.viewRef.get();
        if (imageView == null)
            return;
        apply(request, imageView, drawable);
    }

}

public static abstract class Target<TView> {

    public final WeakReference<TView> viewRef;

    protected Target(TView target) {
        this.viewRef = new WeakReference<>(target);
    }

    public Drawable staticEffect(IconRequest<TView> request, TView view, Drawable drawable) {
        return drawable;
    }

    public abstract void apply(IconRequest<TView> request, TView view, @Nullable Drawable d, boolean animate);

    public abstract boolean forcePlaceholder();

    public abstract Object getTag(TView imageView);

    public abstract Context getContext(TView imageView);

    public abstract void setTag(TView imageView, Object tag);

}

public static abstract class ViewTarget<TView extends View> extends Target<TView> {

    ViewTarget(TView target) {
        super(target);
    }

    @Override
    public Object getTag(TView imageView) {
        return imageView.getTag();
    }

    @Override
    public Context getContext(TView imageView) {
        return imageView.getContext();
    }

    @Override
    public void setTag(TView imageView, Object tag) {
        imageView.setTag(tag);
    }

}

public static class ImageViewTarget extends ViewTarget<ImageView> {

    public ImageViewTarget(ImageView target) {
        super(target);
    }

    @Override
    public Drawable staticEffect(IconRequest<ImageView> request, ImageView view, @Nullable Drawable drawable) {
        if (drawable != null) {
            switch (view.getScaleType()) {
                case CENTER_CROP:
//                    case FIT_CENTER:
                    return new AutoScaledDrawable.In(drawable, request.size.width, request.size.height);
            }
        }
        return drawable;
    }

    @Override
    public void apply(IconRequest<ImageView> request, ImageView view, @Nullable Drawable d, boolean animate) {
        if (animate && d != null) {
            request.log("apply animated");
            GraphicUtils.setImageBitmapAnimated(view, d, IconsManager.CROSS_FADE_REVEAL_DURATION);
        } else {
            request.log("apply static");
            view.setImageDrawable(d);
        }
    }

    @Override
    public boolean forcePlaceholder() {
        return false;
    }

}

public static class TextViewStartTarget extends ViewTarget<TextView> {

    public TextViewStartTarget(TextView target) {
        super(target);
    }

    @Override
    public Drawable staticEffect(IconRequest<TextView> request, TextView view, Drawable drawable) {
        drawable.setBounds(0, 0, request.size.width, request.size.height);
        return drawable;
    }

    @Override
    public void apply(IconRequest<TextView> request, TextView view, @Nullable Drawable d, boolean animate) {
        view.setCompoundDrawablesRelative(d, null, null, null);
    }

    @Override
    public boolean forcePlaceholder() {
        return true;
    }

}
}
