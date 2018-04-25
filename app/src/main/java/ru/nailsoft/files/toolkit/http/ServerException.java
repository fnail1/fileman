package ru.nailsoft.files.toolkit.http;

public class ServerException extends Exception {
    private final int code;
    private final String message;

    public ServerException(int code) {
        super("" + code);
        this.code = code;
        this.message = "";
    }

    public ServerException(int code, String message) {
        super("" + code, new Exception(message));
        this.code = code;
        this.message = message;
    }


    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
