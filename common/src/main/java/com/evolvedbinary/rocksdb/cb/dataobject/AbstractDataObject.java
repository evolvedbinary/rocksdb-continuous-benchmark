package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.*;

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
    public void serialize(final OutputStream os) throws IOException {
        try (final JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            generator.writeStartObject();
            serializeFields(generator);
            generator.writeEndObject();
        }
    }

    abstract void serializeFields(final JsonGenerator generator) throws IOException;

    @Override
    public <T extends DataObject> T deserialize(final String data) throws IOException {
        final byte[] buf = data.getBytes(UTF_8);
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
            return deserialize(bais);
        }
    }

    public <T extends DataObject> T deserialize(final InputStream is) throws IOException {
        try (final JsonParser parser = JSON_FACTORY.createParser(is)) {
            // get the first token
            final JsonToken token = parser.nextToken();

            // Sanity check: verify that we got "Json Object":
            if (token != JsonToken.START_OBJECT) {
                throw new IOException("Expected data to start with an Object");
            }

            return deserializeFields(parser, token);
        }
    }

    abstract <T extends DataObject> T deserializeFields(final JsonParser parser, JsonToken token) throws IOException;
}
