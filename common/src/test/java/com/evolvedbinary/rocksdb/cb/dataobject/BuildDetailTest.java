package com.evolvedbinary.rocksdb.cb.dataobject;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BuildDetailTest {

    @Test
    public void serializeStreamForException() throws IOException {
        final String expected = "{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}";

        final BuildDetail deserialized = BuildDetail.forException(new IOException("message1"));
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserialized.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringForException() throws IOException {
        final String expected = "{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}";

        final BuildDetail deserialized = BuildDetail.forException(new IOException("message1"));
        final String serialized = deserialized.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStreamForStdOut() throws IOException {
        final String expected = "{\"type\":\"STDOUT_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAb3V0LWRhdGG8ODXG\"}";

        final BuildDetail deserialized = BuildDetail.forStdOut("out-data".getBytes(UTF_8));
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserialized.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringForStdOut() throws IOException {
        final String expected = "{\"type\":\"STDOUT_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAb3V0LWRhdGG8ODXG\"}";

        final BuildDetail deserialized = BuildDetail.forStdOut("out-data".getBytes(UTF_8));
        final String serialized = deserialized.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStreamForStdErr() throws IOException {
        final String expected = "{\"type\":\"STDERR_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAZXJyLWRhdGG6rq6w\"}";

        final BuildDetail deserialized = BuildDetail.forStdErr("err-data".getBytes(UTF_8));
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserialized.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringForStdErr() throws IOException {
        final String expected = "{\"type\":\"STDERR_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAZXJyLWRhdGG6rq6w\"}";

        final BuildDetail deserialized = BuildDetail.forStdErr("err-data".getBytes(UTF_8));
        final String serialized = deserialized.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void deserializeStreamForException() throws IOException {
        final BuildDetail expected = BuildDetail.forException(new IOException("message1"));

        final String serialized = "{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}";
        final BuildDetail deserialized = new BuildDetail();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeStringForException() throws IOException {
        final BuildDetail expected = BuildDetail.forException(new IOException("message1"));

        final String serialized = "{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}";
        final BuildDetail deserialized = new BuildDetail().deserialize(serialized);
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeStreamForStdOut() throws IOException {
        final BuildDetail expected = BuildDetail.forStdOut("out-data".getBytes(UTF_8));

        final String serialized = "{\"type\":\"STDOUT_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAb3V0LWRhdGG8ODXG\"}";
        final BuildDetail deserialized = new BuildDetail().deserialize(serialized);
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeStringForStdOut() throws IOException {
        final BuildDetail expected = BuildDetail.forStdOut("out-data".getBytes(UTF_8));

        final String serialized = "{\"type\":\"STDOUT_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAb3V0LWRhdGG8ODXG\"}";
        final BuildDetail deserialized = new BuildDetail().deserialize(serialized);
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeStreamForStdErr() throws IOException {
        final BuildDetail expected = BuildDetail.forStdErr("err-data".getBytes(UTF_8));

        final String serialized = "{\"type\":\"STDERR_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAZXJyLWRhdGG6rq6w\"}";
        final BuildDetail deserialized = new BuildDetail().deserialize(serialized);
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeStringForStdErr() throws IOException {
        final BuildDetail expected = BuildDetail.forStdErr("err-data".getBytes(UTF_8));

        final String serialized = "{\"type\":\"STDERR_LOG\",\"detailCompression\":\"ZSTD\",\"detailEncoding\":\"BASE64\",\"detail\":\"KLUv/SQIQQAAZXJyLWRhdGG6rq6w\"}";
        final BuildDetail deserialized = new BuildDetail().deserialize(serialized);
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeNonJson() {
        assertThrows(IOException.class, () -> {
            new BuildDetail().deserialize("<this-is-not-json/>");
        });
    }

    @Test
    public void deserializeJsonArray() {
        assertThrows(IOException.class, () -> {
            new BuildDetail().deserialize("[]");
        });
    }

    @Test
    public void deserializeUnexpectedJson() {
        assertThrows(IOException.class, () -> {
            new BuildDetail().deserialize("{\"other\":{}}");
        });
    }
}
