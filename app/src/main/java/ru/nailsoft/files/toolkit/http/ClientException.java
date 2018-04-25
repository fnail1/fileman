package ru.nailsoft.files.toolkit.http;

import java.io.IOException;

public class ClientException extends Exception {
    public ClientException(IOException e) {
        super(e);
    }

    public ClientException() {

    }
}
