package ru.nailsoft.files.toolkit;

import org.parceler.Parcel;

@Parcel
public class Flags32 {
    public int data;

    public Flags32() {

    }
    public Flags32(int value) {
        data = value;
    }

    public int get() {
        return data;
    }

    public boolean get(int mask) {
        return mask == (data & mask);
    }

    public void set(int mask, boolean value) {
        if (value)
            data |= mask;
        else
            data &= ~mask;
    }

    public boolean getAndSet(int mask, boolean value){
        int copy = data;
        set(mask, value);
        return (copy == data) == value;
    }

    public void set(int value) {
        data = value;
    }

    @Override
    public String toString() {
        return "0x" + Integer.toHexString(data) + "(" + data + ")";
    }
}
