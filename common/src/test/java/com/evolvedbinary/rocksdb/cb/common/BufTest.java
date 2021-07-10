package com.evolvedbinary.rocksdb.cb.common;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static com.evolvedbinary.rocksdb.cb.common.Buf.Buf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BufTest {

    @Test
    public void equals() {
        Buf buf1 = Buf("hello".getBytes(UTF_8));
        Buf buf2 = Buf("hello".getBytes(UTF_8));
        assertEquals(buf1, buf2);

        buf1 = Buf("hello".getBytes(UTF_8));
        buf2 = Buf("hello1".getBytes(UTF_8));
        assertNotEquals(buf1, buf2);

        buf1 = Buf("hello".getBytes(UTF_8), 1, 2);
        buf2 = Buf("hello".getBytes(UTF_8), 1, 2);
        assertEquals(buf1, buf2);

        buf1 = Buf("hello".getBytes(UTF_8), 0, 2);
        buf2 = Buf("hello".getBytes(UTF_8), 1, 2);
        assertNotEquals(buf1, buf2);
    }
}
