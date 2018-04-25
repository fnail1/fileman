package ru.nailsoft.files.toolkit.http;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ProtocolException;

public interface HttpConnectionBuilder {

    HttpConnectionBuilder setLogger(String tag);

    HttpConnectionBuilder setKeepAlive(boolean keepAlive);

    HttpConnectionBuilder addHeader(String header, String value);

    HttpConnectionBuilder allowRedirects(boolean allow);

    HttpConnectionBuilder setMethod(HttpConnectionImpl.Method method) throws ProtocolException;

    HttpConnectionBuilder setConnectTimeout(int ms);

    HttpConnectionBuilder setReadTimeout(int ms);

    HttpConnectionBuilder setCheckResponseEncoding(boolean checkEncoding);

    HttpConnection build();

    HttpConnection sendChunk(byte[] buffer, int offset, int count, int rangeStart, int total) throws IOException;

    HttpConnection send(FileInputStream inputStream) throws IOException;
}
