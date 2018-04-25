package ru.nailsoft.files.toolkit.collections;

public interface Aggregator<Param, Result>{
    Result invoke(Param p, Result prev);
}

