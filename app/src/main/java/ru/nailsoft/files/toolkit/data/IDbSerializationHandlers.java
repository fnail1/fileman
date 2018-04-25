package ru.nailsoft.files.toolkit.data;

public interface IDbSerializationHandlers {
    void onBeforeSerialization();
    void onAfterDeserialization();
}
