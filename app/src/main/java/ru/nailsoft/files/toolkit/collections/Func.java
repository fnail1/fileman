package ru.nailsoft.files.toolkit.collections;

public interface Func<Param, Result>{
    Result invoke(Param p);
}

