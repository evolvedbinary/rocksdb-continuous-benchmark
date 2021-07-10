package com.evolvedbinary.rocksdb.cb.common;

import java.util.Arrays;

public class Buf {
    public final byte[] data;
    public final int offset;
    public final int length;

    public Buf(final byte[] data, final int offset, final int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public static Buf Buf(final byte[] data, final int offset, final int length) {
        return new Buf(data, offset, length);
    }

    public static Buf Buf(final byte[] data) {
        return new Buf(data, 0, data.length);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Buf buf = (Buf) o;

        if (offset != buf.offset) return false;
        if (length != buf.length) return false;
        return Arrays.equals(data, buf.data);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(data);
        result = 31 * result + offset;
        result = 31 * result + length;
        return result;
    }
}
