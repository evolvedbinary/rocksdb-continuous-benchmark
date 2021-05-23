package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractDataObject implements DataObject {
    protected static final JsonFactory JSON_FACTORY = new JsonFactory();

    @Override
    public String serialize() throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            serialize(baos);
            final byte[] buf = baos.toByteArray();
            return new String(buf, UTF_8);
        }
    }

    @Override
    public <T extends DataObject> T deserialize(final String data) throws IOException {
        final byte[] buf = data.getBytes(UTF_8);
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
            return deserialize(bais);
        }
    }
}
