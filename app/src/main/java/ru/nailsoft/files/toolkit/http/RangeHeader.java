package ru.nailsoft.files.toolkit.http;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.net.HttpURLConnection;

public class RangeHeader {
    private final int start;
    private final int end;
    private final int total;

    @Nullable
    public static RangeHeader parse(HttpURLConnection connection) {
        return parse(connection.getHeaderField("Content-Range"));
    }

    @Nullable
    private static RangeHeader parse(String s) {
        if (TextUtils.isEmpty(s)) {
            return null;
        } else {
            int minus = s.indexOf('-');
            int slash = s.indexOf('/');
            int start = Integer.parseInt(s.substring(6, minus));
            int end = Integer.parseInt(s.substring(minus + 1, slash));
            int total = Integer.parseInt(s.substring(slash + 1));
            return new RangeHeader(start, end, total);
        }
    }

    public RangeHeader(int start, int end, int total) {
        this.start = start;
        this.end = end;
        this.total = total;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getTotal() {
        return total;
    }

    @Override
    public String toString() {
        if (start < total)
            return "bytes " + start + "-" + end + "/" + total;
        else
            return "bytes */" + total;
    }
}
