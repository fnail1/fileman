package ru.nailsoft.files.diagnostics;

public class DebugUtils {
    public static final String NOMAIL_COM_STRING = "@nomail.com";
    public static final int FAKE_CONTACTS_CREATE_COUNT = 10;
    public static boolean isTestUserPhone;


    public static void safeThrow(Throwable e) {
            e.printStackTrace();
    }

}
