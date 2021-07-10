package com.evolvedbinary.rocksdb.cb.common;

import org.junit.jupiter.api.Test;

import static com.evolvedbinary.rocksdb.cb.common.Buf.Buf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class EncodingUtilTest {

    @Test
    public void encodeNoEncoding() {
        final Buf unencoded = Buf("hello".getBytes(UTF_8));
        final Buf encoded = EncodingUtil.encode(unencoded, Encoding.NONE);
        assertEquals(unencoded, encoded);

        final byte[] unencoded2 = EncodingUtil.decode(encoded.data, Encoding.NONE);
        assertArrayEquals(unencoded.data, unencoded2);
    }

    @Test
    public void encodeDecode() {
        final Buf unencoded = Buf("hello".getBytes(UTF_8));
        final Buf encoded = EncodingUtil.encode(unencoded, Encoding.BASE64);
        assertNotNull(encoded.data);
        assertTrue(encoded.data.length > 0);
        assertEquals(0, encoded.offset);
        assertEquals(encoded.data.length, encoded.length);

        final byte[] unencoded2 = EncodingUtil.decode(encoded.data, Encoding.BASE64);
        assertArrayEquals(unencoded.data, unencoded2);
    }
}
