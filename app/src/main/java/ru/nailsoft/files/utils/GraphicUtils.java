package ru.nailsoft.files.utils;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import ru.nailsoft.files.utils.photomanager.adapters.CircleDrawable;
import ru.nailsoft.files.utils.photomanager.adapters.FramedDrawable;


@SuppressWarnings("WeakerAccess")
public class GraphicUtils {


    private static int maxTextureSize;

    private GraphicUtils() {
    }

    public static int getMaxTextureSize() {
        if (maxTextureSize == 0) {
            final int[] buff = new int[1];
            GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, buff, 0);
            maxTextureSize = buff[0];
            if (maxTextureSize == 0) {
                final EGL10 egl = (EGL10) EGLContext.getEGL();
                final EGLContext ctx = egl.eglGetCurrentContext();
                final GL10 gl = (GL10) ctx.getGL();
                gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, buff, 0);
                maxTextureSize = buff[0];
                if (maxTextureSize == 0)
                    maxTextureSize = 2048;
            }
        }

        return maxTextureSize;
    }

    public static Bitmap maskBitmap(Bitmap bitmap, Bitmap mask) {
        if (bitmap == null)
            bitmap = Bitmap.createBitmap(10, 10, Config.ARGB_8888);
        final Bitmap output = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        final Rect maskRect = new Rect(0, 0, mask.getWidth(), mask.getHeight());
        final Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final Rect dstRect = calcDimensions(maskRect, bitmap.getWidth(), bitmap.getHeight(), true, new Rect());
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        canvas.drawBitmap(mask, maskRect, maskRect, paint);

        return output;
    }

    public static Bitmap maskBitmap(Context context, Bitmap bitmap, int maskBitmapId) {
        return maskBitmap(bitmap, getResourceBitmap(context, maskBitmapId));
    }

    public static Bitmap maskBitmap(Context context, int bitmapId, int maskBitmapId) {
        return maskBitmap(context, getResourceBitmap(context, bitmapId), maskBitmapId);
    }

    public static Bitmap getResourceBitmap(Context context, int resId) {
        Drawable drawable = getDrawable(context, resId);
        return toBitmap(drawable);
    }

    public static Drawable getDrawable(Context context, int resId) {
        return AppCompatResources.getDrawable(context, resId);
    }

    public static int calcBitmapSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        // 2048 >= srcWidth / sampleSize >= dstWidth
        // 2048 >= srcWidth / sampleSize >= dstHeight

        final int maxTextureSize = getMaxTextureSize();
        if (dstWidth == 0)
            dstWidth = maxTextureSize;
        else if (dstWidth < 0)
            dstWidth = Integer.MAX_VALUE;
        if (dstHeight == 0)
            dstHeight = maxTextureSize;
        else if (dstHeight < 0)
            dstHeight = Integer.MAX_VALUE;

        if ((srcWidth <= dstWidth) && (srcHeight <= dstHeight))
            return 1;
        else {
            int sampleSize = 1;
            while ((srcWidth > maxTextureSize) || (srcHeight > maxTextureSize)) {
                srcWidth /= 2;
                srcHeight /= 2;
                sampleSize *= 2;
            }
            while (true) {
                srcWidth /= 2;
                srcHeight /= 2;
                if ((srcWidth < dstWidth) || (srcHeight < dstHeight))
                    return sampleSize;
                else
                    sampleSize *= 2;
            }
        }
    }

    public static Bitmap decodeUri(String path, int width, int height) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calcBitmapSampleSize(options.outWidth, options.outHeight, width, height);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }


    public static Bitmap decodeUri(Context context, Uri uri, int width, int height, @Nullable BitmapFactory.Options options, boolean exif) throws IOException {
        if (uri == null)
            return null;

        if (options == null) {
            options = new BitmapFactory.Options();
            options.inPreferredConfig = Config.ARGB_8888;
        }
        options.inJustDecodeBounds = true;

        final String scheme = uri.getScheme();
        final boolean isContentUri = ContentResolver.SCHEME_CONTENT.equals(scheme);
        if (isContentUri) {
            if (context == null)
                throw new InvalidParameterException("Context not provided");

            decodeUriContent(context, uri, options);
        } else {
            if (!ContentResolver.SCHEME_FILE.equals(scheme))
                throw new InvalidParameterException("Unsupported uri scheme: " + uri);

            BitmapFactory.decodeFile(uri.getPath(), options);
        }

        options.inSampleSize = calcBitmapSampleSize(options.outWidth, options.outHeight, width, height);
        options.inJustDecodeBounds = false;

        Bitmap bitmap;
        int rotation = 0;
        if (isContentUri) {
            bitmap = decodeUriContent(context, uri, options);
            if (exif) {
                try {
                    InputStream is = context.getContentResolver().openInputStream(uri);
                    if (is != null) {
                        rotation = getExifOrientation(new ExifInterface(is));
                        is.close();
                    }
                } catch (Exception ignored) {
                }
            }
        } else {
            bitmap = BitmapFactory.decodeFile(uri.getPath(), options);
            if (exif) {
                rotation = getExifOrientation(new ExifInterface(uri.getPath()));
            }
        }

        if (rotation != 0) {
            final Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    private static int getExifOrientation(ExifInterface exif) {
        int rotation = 0;
        final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        if (orientation != ExifInterface.ORIENTATION_NORMAL) {
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
            }
        }

        return rotation;
    }

    private static Bitmap decodeUriContent(Context context, Uri uri, BitmapFactory.Options options) throws IOException {
        final InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null)
            throw new IOException("Input stream is null");

        try {
            return BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            Utils.closeCloseable(inputStream);
        }
    }


    public static Rect projectOut(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        Rect rect = new Rect();
        float scaleW = (float) dstWidth / srcWidth;
        float scaleH = (float) dstHeight / srcHeight;
        // select the maximum scale
        // i.e. float scale = Math.max(scaleW, scaleH);
        if (scaleW > scaleH) {
            // projection dimension is source dimension multiplied on scale (scaleW in this case)
            // i.e. float w = srcWidth * scaleW = srcWidth / (srcWidth / dstWidth) = dstWidth;
            rect.left = 0;
            rect.right = dstWidth;
            float h = srcHeight * scaleW;
            rect.top = (int) ((dstHeight - h) / 2);
            rect.bottom = (int) (rect.top + h);
        } else {
            rect.top = 0;
            rect.bottom = dstHeight;
            float w = srcWidth * scaleH;
            rect.left = (int) ((dstWidth - w) / 2);
            rect.right = (int) (rect.left + w);
        }
        return rect;
    }

    public static Rect projectIn(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        Rect rect = new Rect();
        float scaleW = (float) dstWidth / srcWidth;
        float scaleH = (float) dstHeight / srcHeight;
        // select the maximum scale
        // i.e. float scale = Math.max(scaleW, scaleH);
        if (scaleW < scaleH) {
            // projection dimension is source dimension multiplied on scale (scaleW in this case)
            // i.e. float w = srcWidth * scaleW = srcWidth / (srcWidth / dstWidth) = dstWidth;
            rect.left = 0;
            rect.right = dstWidth;
            float h = srcHeight * scaleW;
            rect.top = (int) ((dstHeight - h) / 2);
            rect.bottom = (int) (rect.top + h);
        } else {
            rect.top = 0;
            rect.bottom = dstHeight;
            float w = srcWidth * scaleH;
            rect.left = (int) ((dstWidth - w) / 2);
            rect.right = (int) (rect.left + w);
        }
        return rect;
    }


    public static Rect calcDimensions(Rect areaRect, int pictureWidth, int pictureHeight, boolean clip, Rect outRect) {
        final float w1 = areaRect.width();
        final float h1 = areaRect.height();
        final float w2 = pictureWidth;
        final float h2 = pictureHeight;
        final boolean isNarrow = (w2 / h2) > (w1 / h1);

        if ((isNarrow && clip) || ((!isNarrow) && (!clip))) {
            outRect.top = areaRect.top;
            outRect.bottom = areaRect.bottom;
            final float w = h1 / h2 * w2;
            outRect.left = (int) ((areaRect.left + areaRect.right - w) / 2f);
            outRect.right = (int) (((float) outRect.left) + w);
        } else {
            outRect.left = areaRect.left;
            outRect.right = areaRect.right;
            final float h = w1 / w2 * h2;
            outRect.top = (int) ((areaRect.top + areaRect.bottom - h) / 2f);
            outRect.bottom = (int) (((float) outRect.top) + h);
        }

        return outRect;
    }

    public static Bitmap getRoundedBitmap(Context context, Bitmap src) {
        Drawable d = getRounded(new BitmapDrawable(context.getResources(), src));
        Bitmap out = Bitmap.createBitmap(d.getBounds().width(), d.getBounds().height(), Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        d.draw(canvas);
        return out;
    }

    public static Drawable getRounded(Drawable src) {
        int srcHeight = src.getIntrinsicHeight();
        int srcWidth = src.getIntrinsicWidth();

        final int x, y, w, h;
        if (srcHeight > srcWidth) {
            h = srcWidth;
            y = (srcHeight - h) / 2;
            x = 0;
            w = srcWidth;
        } else {
            w = srcHeight;
            x = (srcWidth - w) / 2;
            y = 0;
            h = srcHeight;
        }

        Rect srcRect = new Rect(x, y, w + x, h + y);
        Drawable d = new FramedDrawable(src, srcRect);
        return new CircleDrawable(d);
    }

    public static Bitmap toBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable)
            return ((BitmapDrawable) drawable).getBitmap();

        int sw = drawable.getIntrinsicWidth();
        int sh = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(sw, sh, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Rect rect = new Rect(0, 0, sw, sh);
        drawable.setBounds(rect);
        drawable.draw(canvas);
        return bitmap;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setImageBitmapAnimated(final ImageView imageView, Drawable animateTo, int duration) {
        Drawable animateFrom = imageView.getDrawable();


        if (animateFrom == null)
            animateFrom = new ColorDrawable(0);

        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{animateFrom, animateTo});
        transitionDrawable.setCrossFadeEnabled(false);
        transitionDrawable.startTransition(duration);
        imageView.setImageDrawable(transitionDrawable);
    }

    public static Bitmap resizeBitmapToFitBounds(Bitmap srcBitmap, int width, int height, boolean clip) {
        int srcHeight = srcBitmap.getHeight();
        int srcWidth = srcBitmap.getWidth();

        if (clip) {
            final int x, y, w, h;
            float scale;
            if ((srcHeight * width) > (height * srcWidth)) {
                h = height * srcWidth / width;
                y = (srcHeight - h) / 2;
                x = 0;
                w = srcWidth;
                scale = width / srcWidth;
            } else {
                w = width * srcHeight / height;
                x = (srcWidth - w) / 2;
                y = 0;
                h = srcHeight;
                scale = height / srcHeight;
            }

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            return Bitmap.createBitmap(srcBitmap, x, y, w, h, matrix, false);
        } else {
            float scaleW = (float) width / srcWidth;
            float scaleH = (float) height / srcHeight;
            float scale = Math.max(scaleW, scaleH);
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            return Bitmap.createBitmap(srcBitmap, 0, 0, srcWidth, srcHeight, matrix, false);

        }
    }

    public static void compressBitmapToFile(Bitmap bitmap, File file, int quality) throws IOException {
        final OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
        //noinspection TryFinallyCanBeTryWithResources
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            stream.flush();
        } finally {
            stream.close();
        }
    }

}
