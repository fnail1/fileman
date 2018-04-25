package ru.nailsoft.files.utils.photomanager;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.LruCache;

import ru.nailsoft.files.service.AppStateObserver;

import static ru.nailsoft.files.diagnostics.Logger.trace;


class PhotoMemoryCache implements AppStateObserver.LowMemoryEventHandler {
    public static final int MIN_CACHE_SIZE = 4 * 1024 * 1024;
    public static final int MAX_CACHE_SIZE = 16 * 1024 * 1024;
    private final LruCache<String, CacheItem> cache;

    public PhotoMemoryCache(AppStateObserver stateObserver) {
        stateObserver.addLowMemoryEventHandler(this);
        int memCacheSize = (int) (Runtime.getRuntime().maxMemory() * .1F) & 0xfffff000;
        if (memCacheSize < MIN_CACHE_SIZE)
            memCacheSize = MIN_CACHE_SIZE;
        else if (memCacheSize > MAX_CACHE_SIZE)
            memCacheSize = MAX_CACHE_SIZE;


        cache = new LruCache<String, CacheItem>(memCacheSize) {
            @Override
            protected int sizeOf(String key, CacheItem value) {
                Bitmap bitmap = value.bitmap;
                return bitmap == null ? 100 : bitmap.getByteCount() + 100;
            }
        };
    }

    Bitmap get(String key) {
        CacheItem cacheItem = cache.get(key);
        if (cacheItem == null) {
            synchronized (this) {
                cacheItem = cache.get(key);
                if (cacheItem == null)
                    return null;
            }
        }

        return cacheItem.bitmap;
    }

    synchronized void update(String key, Bitmap bitmap) {
        CacheItem cached = cache.get(key);
        if (cached != null) {
            if (cached.bitmap.getWidth() < bitmap.getWidth() || cached.bitmap.getHeight() < bitmap.getHeight()) {
                cache.remove(key);

                cached.bitmap = bitmap;
                cached.timestamp = SystemClock.elapsedRealtime();

                cache.put(key, cached);
            }
            return;
        }

        long t = SystemClock.elapsedRealtime();
        cache.put(key, new CacheItem(bitmap, t));

//        trace(String.format(Locale.US, "count:%d used:%,d", cache.snapshot().size(), cache.size()));

    }

    @Override
    public void onLowMemory() {
        trace();
        cache.evictAll();
    }

    private static class CacheItem {
        Bitmap bitmap;
        long timestamp;

        CacheItem(Bitmap bitmap, long timestamp) {
            this.bitmap = bitmap;
            this.timestamp = timestamp;
        }
    }
}
