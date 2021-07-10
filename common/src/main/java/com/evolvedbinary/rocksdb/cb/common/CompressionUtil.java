package com.evolvedbinary.rocksdb.cb.common;

import io.airlift.compress.zstd.ZstdCompressor;
import io.airlift.compress.zstd.ZstdDecompressor;

import static com.evolvedbinary.rocksdb.cb.common.Buf.Buf;

public interface CompressionUtil {

    static Buf compress(final Buf buf, final Compression compression) {
        if (Compression.NONE == compression) {
            return buf;
        }

        if (Compression.ZSTD != compression) {
            throw new UnsupportedOperationException("Compression " + compression + " is unsupported");
        }

        final ZstdCompressor ztsdCompressor = new ZstdCompressor();
        final int maxCompressedLength = ztsdCompressor.maxCompressedLength(buf.length);

        final byte[] compressed = new byte[maxCompressedLength];
        final int actualCompressedLength = ztsdCompressor.compress(buf.data, buf.offset, buf.length, compressed, 0, maxCompressedLength);

        return Buf(compressed, 0, actualCompressedLength);
    }

    static Buf decompress(final Buf buf, final Compression compression) {
        if (Compression.NONE == compression) {
            return buf;
        }

        if (Compression.ZSTD != compression) {
            throw new UnsupportedOperationException("Compression " + compression + " is unsupported");
        }

        final ZstdDecompressor ztsdDecompressor = new ZstdDecompressor();
        final long maxDecompressedSize = ZstdDecompressor.getDecompressedSize(buf.data, buf.offset, buf.length);
        if (maxDecompressedSize > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Decompression size: " + maxDecompressedSize + "exceeds: " + Integer.MAX_VALUE);
        }

        final byte[] decompressed = new byte[(int)maxDecompressedSize];
        final int actualDecompressedSize = ztsdDecompressor.decompress(buf.data, buf.offset, buf.length, decompressed, 0, (int)maxDecompressedSize);

        return Buf(decompressed, 0, actualDecompressedSize);
    }
}
