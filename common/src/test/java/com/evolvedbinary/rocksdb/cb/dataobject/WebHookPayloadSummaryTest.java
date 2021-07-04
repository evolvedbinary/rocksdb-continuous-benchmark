package com.evolvedbinary.rocksdb.cb.dataobject;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void equalsTest() {
        WebHookPayloadSummary webHookPayloadSummary = new WebHookPayloadSummary();
        assertTrue(webHookPayloadSummary.equals(webHookPayloadSummary));
        assertFalse(webHookPayloadSummary.equals(null));
        assertFalse(webHookPayloadSummary.equals("string"));

        final UUID id = UUID.randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final String ref = "remotes/origin/master";
        final String before = "abc";
        final String after = "def";
        final String repository = "facebook/rocksdb";
        final String pusher = "user1";
        final String sender = "user2";

        webHookPayloadSummary = new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, sender);

        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(null, now, ref, before, after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(UUID.randomUUID(), now, ref, before, after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, null, ref, before, after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, ZonedDateTime.now(), ref, before, after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, null, before, after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, "remotes/origin/other", before, after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, null, after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, "123", after, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, null, repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, "456", repository, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, after, null, pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, after, "org/repo", pusher, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, after, repository, null, sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, after, repository, "pusher", sender)));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, "sender")));
        assertFalse(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, null)));

        assertFalse(new WebHookPayloadSummary(null, now, ref, before, after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(UUID.randomUUID(), now, ref, before, after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, null, ref, before, after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, ZonedDateTime.now(), ref, before, after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, null, before, after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, "remotes/origin/other", before, after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, null, after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, "123", after, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, null, repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, "456", repository, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, after, null, pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, after, "org/repo", pusher, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, after, repository, null, sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, after, repository, "pusher", sender).equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, "sender").equals(webHookPayloadSummary));
        assertFalse(new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, null).equals(webHookPayloadSummary));

        assertTrue(webHookPayloadSummary.equals(new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, sender)));
    }

    @Test
    public void hashCodeTest() {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final String ref = "remotes/origin/master";
        final String before = "abc";
        final String after = "def";
        final String repository = "facebook/rocksdb";
        final String pusher = "user1";
        final String sender = "user2";

        assertNotEquals(0, new WebHookPayloadSummary(null, now, ref, before, after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(UUID.randomUUID(), now, ref, before, after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, null, ref, before, after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, ZonedDateTime.now(), ref, before, after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, null, before, after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, "remotes/origin/other", before, after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, null, after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, "123", after, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, null, repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, "456", repository, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, after, null, pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, after, "org/repo", pusher, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, after, repository, null, sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, after, repository, "pusher", sender).hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, "sender").hashCode());
        assertNotEquals(0, new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, null).hashCode());
    }

    @Test
    public void getters() {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final String ref = "remotes/origin/master";
        final String before = "abc";
        final String after = "def";
        final String repository = "facebook/rocksdb";
        final String pusher = "user1";
        final String sender = "user2";

        final WebHookPayloadSummary webHookPayloadSummary = new WebHookPayloadSummary(id, now, ref, before, after, repository, pusher, sender);

        assertEquals(id, webHookPayloadSummary.getId());
        assertEquals(now, webHookPayloadSummary.getTimeStamp());
        assertEquals(ref, webHookPayloadSummary.getRef());
        assertEquals(before, webHookPayloadSummary.getBefore());
        assertEquals(after, webHookPayloadSummary.getAfter());
        assertEquals(repository, webHookPayloadSummary.getRepository());
        assertEquals(pusher, webHookPayloadSummary.getPusher());
        assertEquals(sender, webHookPayloadSummary.getSender());
    }
}
