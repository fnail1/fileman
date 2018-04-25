package ru.nailsoft.files.toolkit.http;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import ru.nailsoft.files.toolkit.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ru.nailsoft.files.diagnostics.DebugUtils.safeThrow;
import static ru.nailsoft.files.diagnostics.Logger.logV;
import static ru.nailsoft.files.toolkit.io.FileUtils.safeClose;


/*package local*/ class HttpConnectionImpl extends HttpConnection implements HttpConnectionBuilder {
    public static final String RANGE_HEADER_FORMAT = "%d-%d/%d";

    private final String url;
    private final HttpURLConnection connection;
    private boolean checkResponseEncoding;
    private String debugLogTag;
    private boolean requestLogged;
    private boolean responseLogged;
    private volatile boolean forceClosed;

    /*package local*/ HttpConnectionImpl(String url) throws ClientException, IOException {
        this(new URL(url));
    }

    /*package local*/ HttpConnectionImpl(URL url) throws ClientException, IOException {
        this.url = url.toString();
        connection = (HttpURLConnection) url.openConnection();
        setConnectTimeout(30 * 1000);
        setReadTimeout(30 * 1000);
        allowRedirects(false);

    }

    public void emptyAndClose(InputStream inputStream) throws IOException {
        if (inputStream == null)
            return;

        try {
            byte[] buf = new byte[1024];
            //noinspection StatementWithEmptyBody
            while (inputStream.read(buf) >= 0) ;
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                logV(debugLogTag, ex.toString());
            }
        }
    }

    @Override
    public HttpConnectionBuilder setMethod(Method method) throws ProtocolException {
        switch (method) {
            case GET:
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.setDoOutput(false);
                break;
            case POST:
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                break;
            case HEAD:
                connection.setRequestMethod("HEAD");
                connection.setDoInput(false);
                connection.setDoOutput(false);
                break;
        }
        return this;
    }

    @Override
    public HttpConnectionBuilder setKeepAlive(boolean keepAlive) {
        if (keepAlive)
            connection.addRequestProperty("Connection", "Keep-Alive");
        else
            connection.addRequestProperty("Connection", "Close");
        return this;
    }

    @Override
    public HttpConnectionBuilder setLogger(String tag) {
        debugLogTag = tag;
        return this;
    }

    @Override
    public HttpConnectionBuilder allowRedirects(boolean allow) {
        connection.setInstanceFollowRedirects(allow);
        return this;
    }

    @Override
    public HttpConnectionBuilder setConnectTimeout(int ms) {
        connection.setConnectTimeout(ms);
        return this;
    }

    @Override
    public HttpConnectionBuilder setReadTimeout(int ms) {
        connection.setReadTimeout(ms);
        return this;
    }

    @Override
    public HttpConnectionBuilder addHeader(String header, String value) {
        connection.addRequestProperty(header, value);
        return this;
    }

    @Override
    public HttpConnectionBuilder setCheckResponseEncoding(boolean checkEncoding) {
        this.checkResponseEncoding = checkEncoding;
        return this;
    }

    @Override
    public HttpConnection build() {
        return this;
    }

    @Override
    public int getResponseCode() throws IOException {
        logRequest("HttpConnection.getResponseCode");
        try {
            int responseCode = connection.getResponseCode();
            logResponse("HttpConnection.getResponseCode'1");
            return responseCode;
        } catch (IOException e) {
            int responseCode = connection.getResponseCode();
            logResponse("HttpConnection.getResponseCode'2");
            return responseCode;
        }
    }

    @Override
    public String getContentType() {
        logRequest("HttpConnection.getContentType");
        String contentType = connection.getContentType();
        logResponse("HttpConnection.getContentType");
        return contentType;
    }

    @Override
    public int getContentLength() {
        logRequest("HttpConnection.getContentLength");
        int contentLength = connection.getContentLength();
        logResponse("HttpConnection.getContentLength");
        return contentLength;
    }

    @Override
    public String getResponseAsString() throws IOException {
        logRequest("HttpConnection.getResponseAsString");
        try {
            InputStream inputStream = getInputStream();
            return readAsString(inputStream);
        } finally {
            forceClose();
        }
    }

    @NonNull
    private String readAsString(InputStream inputStream) throws IOException {
        try {
            String charset = "UTF-8";
            if (checkResponseEncoding) {
                String contentType = getHeaderField("Content-Type");
                if (contentType != null) {
                    for (String param : contentType.replace(" ", "").split(";")) {
                        if (param.startsWith("charset=")) {
                            charset = param.split("=", 2)[1];
                            break;
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder(1024);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charset);
            try {
                BufferedReader reader = new BufferedReader(inputStreamReader);
                try {
                    for (String line; (line = reader.readLine()) != null; ) {
                        sb.append(line);
                    }
                } finally {
                    reader.close();
                }
            } finally {
                inputStreamReader.close();
            }

            String response = sb.toString();
            logResponse("HttpConnection.getResponseAsString");
            log("HttpConnection.getResponseAsString", response);
            return response;
        } finally {
            inputStream.close();
        }
    }

    @Override
    public HttpConnection sendChunk(byte[] buffer, int offset, int count, int rangeStart, int total) throws IOException {
        String range = String.format(Locale.US, RANGE_HEADER_FORMAT, rangeStart, rangeStart + count - 1, total);
        connection.addRequestProperty("Accept-Ranges", "bytes");
        connection.addRequestProperty("Content-Range", range);
        setKeepAlive(rangeStart + count < total);
        connection.setFixedLengthStreamingMode(count);
        logRequest("HttpConnection.sendChunk");

        connection.connect();
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(buffer, offset, count);
            outputStream.flush();
        } finally {
            safeClose(outputStream);
        }
        logResponse("HttpConnection.sendChunk");
        return this;
    }

    @Override
    public void emptyAndClose() {
        if (forceClosed)
            return;

        logRequest("HttpConnection.emptyAndClose");

        try {
            emptyAndClose(connection.getInputStream());
        } catch (IOException e) {
            log("HttpConnection.emptyAndClose", e.toString());
        }

        try {
            emptyAndClose(connection.getErrorStream());
        } catch (IOException e) {
            log("HttpConnection.emptyAndClose", e.toString());
        }

        logResponse("HttpConnection.emptyAndClose");
        forceClose();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        logRequest("HttpConnection.getInputStream");
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
            try {
                emptyAndClose(connection.getErrorStream());
            } catch (IOException e) {
                log("getInputStream''1", e.toString());
            }
        } catch (FileNotFoundException e) {
            inputStream = connection.getErrorStream();
            log("getInputStream''2", e.toString());
            if (inputStream == null)
                throw new IOException("errorStream is null");
        }
        logResponse("HttpConnection.getInputStream");
        return inputStream;
    }

    @Override
    public String getHeaderField(String s) {
        logRequest("HttpConnection.getHeaderField");
        String field = connection.getHeaderField(s);
        logResponse("HttpConnection.getHeaderField");
        return field;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        logRequest("HttpConnection.getHeaderFields");
        Map<String, List<String>> fields = connection.getHeaderFields();
        logResponse("HttpConnection.getHeaderFields");
        return fields;
    }

    @Override
    public File downloadFile(String path, String tempPath, boolean overwrite)
            throws
            IOException,
            ServerException {
        try {
            File file = new File(path);
            if (file.exists()) {
                if (overwrite) {
                    if (!file.delete()) {
                        safeThrow(new Exception("Failed to remove file.", new Exception(path)));
                    }
                } else {
                    return file;
                }
            }

            File tempFile = new File(tempPath);
            if (tempFile.exists() && tempFile.length() > 0 && !tempFile.delete()) {
                safeThrow(new Exception("Failed to remove temp file.", new Exception(tempPath)));
            }

            FileOutputStream outputStream = new FileOutputStream(tempFile);
            try {
                downloadContent(outputStream);
            } finally {
                try {
                    outputStream.getFD().sync();
                } catch (IOException e) {
                    log("HttpConnection.downloadFile", e.toString());
                }
                safeClose(outputStream);
            }

            try {
                FileUtils.rename(tempFile, file);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to rename temp file:" + tempPath, e);
            }

            return file;
        } finally {
            forceClose();
        }
    }

    @Override
    public void downloadContent(OutputStream outputStream)
            throws
            IOException,
            ServerException {
        logRequest("HttpConnection.downloadContent");
        try {
            int responseCode = getResponseCode();
            logResponse("HttpConnection.downloadContent");

            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                emptyAndClose();
                throw new ServerException(responseCode);
            }

            InputStream inputStream = getInputStream();
            int contentLength = connection.getContentLength();
            if (contentLength <= 0)
                contentLength = 16 * 1024;
            byte[] buf = FileUtils.allocateBuffer(contentLength);
            int count;
            while (!forceClosed && (count = inputStream.read(buf)) >= 0) {
                outputStream.write(buf, 0, count);
            }
        } catch (IOException e) {
            if (!forceClosed)
                throw e;
        } finally {
            emptyAndClose();
        }
    }

    @Override
    public RangeHeader getResponseRange() {
        String responseRange = getHeaderField("Content-Range");
        if (TextUtils.isEmpty(responseRange)) {
            return null;
        } else {
            int minus = responseRange.indexOf('-');
            int slash = responseRange.indexOf('/');
            int total = Integer.parseInt(responseRange.substring(slash + 1));
            if (minus > 0) {
                int start = Integer.parseInt(responseRange.substring(6, minus));
                int end = Integer.parseInt(responseRange.substring(minus + 1, slash));
                return new RangeHeader(start, end, total);
            } else {
                return new RangeHeader(total, total, total);
            }
        }
    }

    @Override
    public void forceClose() {
        if (forceClosed)
            return;
        forceClosed = true;
        connection.disconnect();
    }

    @Override
    public void prepareForImageLoading() {
        connection.setUseCaches(false);
    }

    @Override
    public void connect() throws IOException {
        logRequest("HttpConnection.connect");
        connection.connect();
    }

    @Override
    public HttpConnection send(FileInputStream inputStream) throws IOException {
        int length = inputStream.available();
        connection.setFixedLengthStreamingMode(length);
        logRequest("send");
        connection.connect();

        OutputStream outputStream = connection.getOutputStream();
        try {
            byte[] buf = FileUtils.allocateBuffer(8 * 1024);
            int count;
            int total = 0;
            while (total < length && (count = inputStream.read(buf)) >= 0) {
                outputStream.write(buf, 0, Math.min(count, length - total));
                total += count;
            }
            outputStream.flush();
        } finally {
            safeClose(outputStream);
        }

        logResponse("send");
        return this;
    }

    private void logRequest(String method) {
        if (debugLogTag != null) {
            if (requestLogged)
                return;
            requestLogged = true;
            try {
                log(method, url);
                StringBuilder stringBuilder = new StringBuilder();
                for (String header : connection.getRequestProperties().keySet()) {
                    stringBuilder.append(header).append(" : ").append(connection.getRequestProperty(header)).append('\n');
                }
                log(method, stringBuilder.toString());
            } catch (Exception ignored) {
            }
        }
    }

    private void logResponse(String method) {
        if (debugLogTag != null) {
            if (responseLogged)
                return;
            responseLogged = true;
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("contentLength : ").append(connection.getContentLength()).append('\n');
                for (String header : connection.getHeaderFields().keySet()) {
                    stringBuilder.append(header).append(" : ").append(connection.getHeaderField(header)).append('\n');
                }
                log(method, stringBuilder.toString());
            } catch (Exception ignored) {
            }
        }
    }

    private void log(String method, String message) {
        if (debugLogTag == null)
            return;
        logV(debugLogTag, "%s: %s", method, message);
    }

}
