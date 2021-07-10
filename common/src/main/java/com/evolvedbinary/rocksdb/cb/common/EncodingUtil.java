package com.evolvedbinary.rocksdb.cb.common;

import org.apache.commons.codec.binary.Base64;

import static com.evolvedbinary.rocksdb.cb.common.Buf.Buf;

public interface EncodingUtil {

    static Buf encode(final Buf buf, final Encoding encoding) {
        if (Encoding.NONE == encoding) {
            return buf;
        }

        if (Encoding.BASE64 != encoding) {
            throw new UnsupportedOperationException("Encoding " + encoding + " is unsupported");
        }

        final Base64 base64 = new Base64();
        final byte[] encoded = base64.encode(buf.data, buf.offset, buf.length);

        return Buf(encoded);
    }

    static byte[] decode(final byte[] data, final Encoding encoding) {
        if (Encoding.NONE == encoding) {
            return data;
        }

        if (Encoding.BASE64 != encoding) {
            throw new UnsupportedOperationException("Encoding " + encoding + " is unsupported");
        }

        final Base64 base64 = new Base64();
        return base64.decode(data);
    }
}
