package ru.nailsoft.files.toolkit.http;

import android.support.annotation.Nullable;

import ru.nailsoft.files.toolkit.io.FileOpException;
import ru.nailsoft.files.toolkit.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import static ru.nailsoft.files.diagnostics.DebugUtils.safeThrow;

public class HttpUtils {
    public static final int MAX_ATTEMPTS_NUMBER = 3;

    public static String downloadFile(URL url, File file, String etag) throws IOException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_NUMBER; attempt++) {
            long rangeStart = file.exists() ? file.length() : 0;
            try {
                return downloadFileInternal(url, file, etag, rangeStart);
            } catch (IOException e) {
//                if (!networkObserver().isNetworkAvailable())
//                    throw e;
                if (attempt == MAX_ATTEMPTS_NUMBER - 1)
                    throw e;
            } catch (TryAgainException e) {
                etag = null;
                attempt--;
            } catch (ClientException | ServerException e) {
                return null;
            } catch (Exception e) {
                safeThrow(e);
            }
        }
        return null;
    }

    @Nullable
    private static String downloadFileInternal(URL url, File file, String etag, long rangeStart) throws IOException, ClientException, TryAgainException, ServerException {
        HttpConnectionBuilder connectionBuilder = HttpConnection.builder(url)
                .setMethod(HttpConnection.Method.GET)
                .setKeepAlive(true)
                .setLogger("DOWNLOAD");

        if (rangeStart > 0) {
            connectionBuilder.addHeader("Range", String.format(Locale.US, "bytes=%d-", rangeStart));
        } else if (etag != null) {
            connectionBuilder.addHeader("If-None-Match", etag);
        }

        HttpConnection connection = connectionBuilder.build();
        try {

            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                case 416:   // Requested Range Not Satisfiable
                    break;
                case HttpURLConnection.HTTP_NOT_MODIFIED:
                    return null;
                default:
                    throw new ServerException(connection.getResponseCode());
            }

            if (rangeStart > 0) {
                RangeHeader range = connection.getResponseRange();
                if (range == null || range.getStart() != rangeStart ||
                        (etag != null && !etag.equals(connection.getHeaderField("ETag")))) {
                    if (!file.delete()) {
                        safeThrow(new FileOpException(FileOpException.FileOp.DELETE, file));
                        return null;
                    }
                    if (range == null || range.getStart() == 0)
                        rangeStart = 0;
                    else
                        throw new TryAgainException();
                }
            }
            long contentLength = connection.getContentLength();
            int bufSize = 0 < contentLength && contentLength < 16 * 1024 ? (int) contentLength : 16 * 1024;
            byte[] buf = new byte[bufSize];

            FileOutputStream outputStream = FileUtils.openFileOutputStream(file, rangeStart > 0);
            try {
                FileUtils.copyStream(connection.getInputStream(), outputStream, buf);
            } finally {
                outputStream.close();
            }
            return connection.getHeaderField("ETag");
        } finally {
            connection.emptyAndClose();
        }
    }

    private static class TryAgainException extends Exception {
    }
}
