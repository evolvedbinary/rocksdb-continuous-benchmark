package com.evolvedbinary.rocksdb.cb.common;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static com.evolvedbinary.rocksdb.cb.common.Buf.Buf;
import static org.junit.jupiter.api.Assertions.*;

public class CompressionUtilTest {

    @Test
    public void compressDecompressNoCompression() {
        final Buf uncompressed = Buf("hello".getBytes(UTF_8));
        final Buf compressed = CompressionUtil.compress(uncompressed, Compression.NONE);
        assertEquals(uncompressed, compressed);

        final Buf uncompressed2 = CompressionUtil.decompress(compressed, Compression.NONE);
        assertEquals(uncompressed, uncompressed2);
    }

    @Test
    public void compressDecompress() {
        final Buf uncompressed = Buf("hello".getBytes(UTF_8));
        final Buf compressed = CompressionUtil.compress(uncompressed, Compression.ZSTD);
        assertNotNull(compressed.data);
        assertTrue(compressed.data.length > 0);
        assertEquals(0, compressed.offset);
        assertTrue(compressed.data.length >= compressed.length);

        final Buf uncompressed2 = CompressionUtil.decompress(compressed, Compression.ZSTD);
        assertEquals(uncompressed, uncompressed2);
    }
}
