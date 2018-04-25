package ru.nailsoft.files.toolkit.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class HttpConnection {

    public static HttpConnectionBuilder builder(String url) throws IOException, ClientException {
        return new HttpConnectionImpl(url);
    }


    public static HttpConnectionBuilder builder(URL url) throws IOException, ClientException {
        return new HttpConnectionImpl(url);
    }

    public static File downloadImage(String url, String path, boolean overwrite) throws Exception {
        File image = new HttpConnectionImpl(url)
                .setMethod(Method.GET)
                .setKeepAlive(false)
                .setLogger("HttpConnection.downloadImage")
                .build()
                .downloadFile(path, path + ".tmp", overwrite);
//        if (TextUtils.isEmpty(AvatarLoader.getImageMimeType(previewUrl))) {
//            //noinspection ResultOfMethodCallIgnored
//            previewUrl.delete();
//            throw new Exception("Loaded file is not a supported previewUrl");
//        }
        return image;
    }

    /*package local*/ HttpConnection() {
    }

    public abstract File downloadFile(String path, String tempPath, boolean overwrite) throws IOException, ServerException;

    public abstract int getResponseCode() throws IOException;

    public abstract Map<String, List<String>> getHeaderFields();

    public abstract String getHeaderField(String header);

    public abstract void emptyAndClose();

    public abstract int getContentLength();

    public abstract InputStream getInputStream() throws IOException;

    public abstract String getContentType();

    public abstract String getResponseAsString() throws IOException;

    public abstract void downloadContent(OutputStream outputStream) throws IOException, ServerException;

    public abstract RangeHeader getResponseRange();

    public abstract void forceClose();

    public abstract void prepareForImageLoading();

    public abstract void connect() throws IOException;


    public enum Method {
        GET, POST, HEAD
    }

}
