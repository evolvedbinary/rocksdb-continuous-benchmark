package com.evolvedbinary.rocksdb.cb.dataobject;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebHookPayloadSummaryTest {

    @Test
    public void serializeStream() throws IOException {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime timeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"ref\":\"origin/refs/master\",\"before\":\"abc\",\"after\":\"def\",\"repository\":\"facebook/rocksdb\",\"pusher\":\"person1\",\"sender\":\"person2\"}";

        final WebHookPayloadSummary deserialized = new WebHookPayloadSummary(id, timeStamp, "origin/refs/master", "abc", "def", "facebook/rocksdb", "person1", "person2");
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserialized.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeString() throws IOException {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime timeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"ref\":\"origin/refs/master\",\"before\":\"abc\",\"after\":\"def\",\"repository\":\"facebook/rocksdb\",\"pusher\":\"person1\",\"sender\":\"person2\"}";

        final WebHookPayloadSummary deserialized = new WebHookPayloadSummary(id, timeStamp, "origin/refs/master", "abc", "def", "facebook/rocksdb", "person1", "person2");
        final String serialized = deserialized.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void deserializeStream() throws IOException {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime timeStamp = ZonedDateTime.now();

        final WebHookPayloadSummary expected = new WebHookPayloadSummary(id, timeStamp, "origin/refs/master", "abc", "def", "facebook/rocksdb", "person1", "person2");

        final String serialized = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"ref\":\"origin/refs/master\",\"before\":\"abc\",\"after\":\"def\",\"repository\":\"facebook/rocksdb\",\"pusher\":\"person1\",\"sender\":\"person2\"}";
        final WebHookPayloadSummary deserialized = new WebHookPayloadSummary();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeString() throws IOException {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime timeStamp = ZonedDateTime.now();

        final WebHookPayloadSummary expected = new WebHookPayloadSummary(id, timeStamp, "origin/refs/master", "abc", "def", "facebook/rocksdb", "person1", "person2");

        final String serialized = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"ref\":\"origin/refs/master\",\"before\":\"abc\",\"after\":\"def\",\"repository\":\"facebook/rocksdb\",\"pusher\":\"person1\",\"sender\":\"person2\"}";
        final WebHookPayloadSummary deserialized = new WebHookPayloadSummary().deserialize(serialized);
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeNonJson() {
        assertThrows(IOException.class, () -> {
            new WebHookPayloadSummary().deserialize("<this-is-not-json/>");
        });
    }

    @Test
    public void deserializeJsonArray() {
        assertThrows(IOException.class, () -> {
            new WebHookPayloadSummary().deserialize("[]");
        });
    }

    @Test
    public void deserializeUnexpectedJson() {
        final UUID id = UUID.randomUUID();
        assertThrows(IOException.class, () -> {
            new WebHookPayloadSummary().deserialize("{\"id\":\"" + id.toString() + "\",\"other\":{}}");
        });
    }
}
