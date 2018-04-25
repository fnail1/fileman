package ru.nailsoft.files.toolkit.collections;

public interface LongSelector<Item> {
    long invoke(Item item);
}
