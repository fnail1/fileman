package ru.nailsoft.files.utils.photomanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextPaint;
import android.view.View;
import android.widget.ImageView;

import ru.nailsoft.files.BuildConfig;
import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.service.AppStateObserver;
import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.ui.ScreenMetrics;
import ru.nailsoft.files.utils.GraphicUtils;
import ru.nailsoft.files.utils.Utils;
import ru.nailsoft.files.utils.photomanager.adapters.AutoScaledDrawable;

import static ru.nailsoft.files.App.screenMetrics;


public class IconsManager {
    private static final String CACHE_DIR_NAME = "photos_cache";
    static final int CROSS_FADE_REVEAL_DURATION = 500;
    final PhotoMemoryCache cache;
    private final Drawable document;
    private final Bitmap file;
    private final TextPaint extPaint;
    private final Drawable folder;

    public IconsManager(Context context, AppStateObserver stateObserver, ScreenMetrics screenMetrics) {
        cache = new PhotoMemoryCache(stateObserver);

        Drawable d = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_document, context.getTheme());
        document = new AutoScaledDrawable.In(d, screenMetrics.icon.width, screenMetrics.icon.height);

        d = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_file, context.getTheme());
        file = Bitmap.createBitmap(screenMetrics.icon.width, screenMetrics.icon.height, Bitmap.Config.ARGB_8888);
        d = new AutoScaledDrawable.In(d, screenMetrics.icon.width, screenMetrics.icon.height);
        Canvas canvas = new Canvas(file);
        d.setBounds(0, 0, screenMetrics.icon.width, screenMetrics.icon.height);
        d.draw(canvas);
        extPaint = new TextPaint();
        extPaint.setColor(Utils.getColor(context, R.color.colorAccent));
        extPaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.icon_gen_ext_size));

        folder = GraphicUtils.getDrawable(context, R.drawable.ic_folder);
    }

    public PhotoRequestBuilder<ImageView> attach(ImageView imageView, FileItem file) {
        return attach(new IconRequest.ImageViewTarget(imageView), file);
    }

    public <TView extends View> PhotoRequestBuilder<TView> attach(IconRequest.Target<TView> imageView, FileItem file) {
        PhotoRequestBuilder<TView> builder = new PhotoRequestBuilder<>(this, imageView, file);

        if (BuildConfig.DEBUG) {

            IllegalStateException th = new IllegalStateException("commit() not called!");
            ThreadPool.UI.post(() -> {
                if (!builder.committed)
                    throw th;
            });
        }


        return builder;
    }


    @UiThread
    void attach(IconRequest iconRequest) {
        if (iconRequest.bind())
            iconRequest.start();
    }

    @UiThread
    public void clean(ImageView imageView) {
        imageView.setTag(null);
    }

    @NonNull
    Bitmap generateForExtension(String ext) {
        if (ext == null || ext.isEmpty() || ext.length() > 4)
            return file;

        Bitmap bitmap = Bitmap.createBitmap(screenMetrics().icon.width, screenMetrics().icon.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        document.draw(canvas);

        float w = extPaint.measureText(ext);
        canvas.drawText(ext,
                (bitmap.getWidth() - w) / 2,
                bitmap.getHeight() - extPaint.getTextSize(),
                extPaint);

        return bitmap;
    }

    public Drawable getFolderIcon() {
        return folder;
    }
}
