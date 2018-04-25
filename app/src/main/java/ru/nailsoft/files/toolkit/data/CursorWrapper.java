package ru.nailsoft.files.toolkit.data;

import android.database.Cursor;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public abstract class CursorWrapper<T> implements Closeable, Collection<T> {
    protected final Cursor cursor;

    public CursorWrapper(Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public int size() {
        return cursor.getCount();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    public boolean moveToFirst() {
        return cursor.moveToFirst();
    }

    public boolean moveToNext() {
        return cursor.moveToNext();
    }


    public final T get() {
        return get(cursor);
    }

    protected abstract T get(Cursor cursor);

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            boolean hasNext = moveToFirst();

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public T next() {
                try {
                    return get();
                } finally {
                    hasNext = moveToNext();
                }
            }
        };
    }

    @NonNull
    @Override
    public Object[] toArray() {
        Object[] array = new Object[size()];
        int idx = 0;
        for (T t : this)
            array[idx++] = t;
        return array;
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] a) {
        int size = size();
        if (a.length > size)
            a = Arrays.copyOf(a, size);
        int idx = 0;
        for (T t : this)
            a[idx++] = (T1) t;
        return a;
    }

    @Override
    public boolean add(T t) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public void close() {
        cursor.close();
    }


    public T readSingleAndClose() {
        try {
            return moveToFirst() ? get() : null;
        } finally {
            close();
        }
    }

    @NonNull
    public ArrayList<T> asList() {
        try {
            return toList();
        } finally {
            close();
        }
    }

    @NonNull
    public ArrayList<T> toList() {
        return new ArrayList<>(this);
    }

    @NonNull
    public <K> HashMap<K, T> asMap(Selector<T, K> keySelector) {
        try {
            return toMap(keySelector);
        } finally {
            close();
        }
    }

    @NonNull
    public <K> HashMap<K, T> toMap(Selector<T, K> keySelector) {
        HashMap<K, T> list = new HashMap<>(size());
        for (T item : this) {
            K key = keySelector.select(item);
            list.put(key, item);
        }

        return list;
    }

    @NonNull
    public LongSparseArray<T> asLongSparseArray(LongSelector<T> keySelector) {
        try {
            return toLongSparseArray(keySelector);
        } finally {
            close();
        }
    }

    @NonNull
    public LongSparseArray<T> toLongSparseArray(LongSelector<T> keySelector) {
        LongSparseArray<T> list = new LongSparseArray<>(size());
        try {
            if (cursor.moveToFirst()) {
                do {
                    T obj = get();
                    long key = keySelector.select(obj);
                    list.put(key, obj);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return list;

    }

    public T random() {
        try {
            int size = size();
            if (size == 0)
                return null;
            int rnd = (int) ((SystemClock.elapsedRealtime() >> 6) % size);
            cursor.move(rnd);
            return get();
        } finally {
            cursor.close();
        }
    }

    public static <T> CursorWrapper<T> wrap(Cursor cursor, Class<T> rawType, String tableAlias) {
        return new CursorWrapper<T>(cursor) {
            private final Field[] map = DbUtils.mapCursorForRawType(cursor, rawType, tableAlias);

            @Override
            protected T get(Cursor cursor) {
                //noinspection TryWithIdenticalCatches
                try {
                    return DbUtils.readObjectFromCursor(cursor, rawType.newInstance(), map);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public interface LongSelector<T> {
        long select(T obj);
    }

    public interface Selector<T, K> {
        K select(T obj);
    }
}
