package com.evolvedbinary.rocksdb.cb.dataobject;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BuildRequestTest {

    @Test
    public void serializeStream() throws IOException {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime timeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}";

        final BuildRequest deserialized = new BuildRequest(id, timeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
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

        final String expected = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}";

        final BuildRequest deserialized = new BuildRequest(id, timeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final String serialized = deserialized.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void deserializeStream() throws IOException {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime timeStamp = ZonedDateTime.now();

        final BuildRequest expected = new BuildRequest(id, timeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");

        final String serialized = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}";
        final BuildRequest deserialized = new BuildRequest();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeString() throws IOException {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime timeStamp = ZonedDateTime.now();

        final BuildRequest expected = new BuildRequest(id, timeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");

        final String serialized = "{\"id\":\"" + id.toString() + "\",\"timeStamp\":\"" + timeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}";
        final BuildRequest deserialized = new BuildRequest().deserialize(serialized);
        assertEquals(expected, deserialized);
    }

    @Test
    public void deserializeNonJson() {
        assertThrows(IOException.class, () -> {
            new BuildRequest().deserialize("<this-is-not-json/>");
        });
    }

    @Test
    public void deserializeJsonArray() {
        assertThrows(IOException.class, () -> {
            new BuildRequest().deserialize("[]");
        });
    }

    @Test
    public void deserializeUnexpectedJson() {
        final UUID id = UUID.randomUUID();
        assertThrows(IOException.class, () -> {
            new BuildRequest().deserialize("{\"id\":\"" + id.toString() + "\",\"other\":{}}");
        });
    }

    @Test
    public void equalsTest() {
        BuildRequest buildRequest = new BuildRequest();
        assertTrue(buildRequest.equals(buildRequest));
        assertFalse(buildRequest.equals(null));
        assertFalse(buildRequest.equals("string"));

        final UUID id = UUID.randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final String repository = "facebook/rocksdb";
        final String ref = "remotes/origin/master";
        final String commit = "abcdef1";
        final String author = "user1";

        buildRequest = new BuildRequest(id, now, repository, ref, commit, author);

        assertFalse(buildRequest.equals(new BuildRequest(null, now, repository, ref, commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(UUID.randomUUID(), now, repository, ref, commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, null, repository, ref, commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, ZonedDateTime.now(), repository, ref, commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, null, ref, commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, "org/repo", ref, commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, repository, null, commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, repository, "remotes/origin/other", commit, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, repository, "remotes/origin/other", null, author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, repository, "remotes/origin/other", "1defcba", author)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, repository, "remotes/origin/other", commit, null)));
        assertFalse(buildRequest.equals(new BuildRequest(id, now, repository, "remotes/origin/other", commit, "other")));

        assertFalse(new BuildRequest(null, now, repository, ref, commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(UUID.randomUUID(), now, repository, ref, commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, null, repository, ref, commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, ZonedDateTime.now(), repository, ref, commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, null, ref, commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, "org/repo", ref, commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, repository, null, commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, repository, "remotes/origin/other", commit, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, repository, "remotes/origin/other", null, author).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, repository, "remotes/origin/other", "1defcba", author).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, repository, "remotes/origin/other", commit, null).equals(buildRequest));
        assertFalse(new BuildRequest(id, now, repository, "remotes/origin/other", commit, "other").equals(buildRequest));

        assertTrue(buildRequest.equals(new BuildRequest(id, now, repository, ref, commit, author)));
    }

    @Test
    public void hashCodeTest() {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final String repository = "facebook/rocksdb";
        final String ref = "remotes/origin/master";
        final String commit = "abcdef1";
        final String author = "user1";

        assertNotEquals(0, new BuildRequest(null, now, repository, ref, commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(UUID.randomUUID(), now, repository, ref, commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, null, repository, ref, commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, ZonedDateTime.now(), repository, ref, commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, null, ref, commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, "org/repo", ref, commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, repository, null, commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, repository, "123", commit, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, repository, ref, null, author).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, repository, ref, "1fedcba", author).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, repository, ref, commit, null).hashCode());
        assertNotEquals(0, new BuildRequest(id, now, repository, ref, commit, "other").hashCode());
    }

    @Test
    public void getters() {
        final UUID id = UUID.randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final String repository = "facebook/rocksdb";
        final String ref = "remotes/origin/master";
        final String commit = "abcdef1";
        final String author = "user1";

        final BuildRequest buildRequest = new BuildRequest(id, now, repository, ref, commit, author);

        assertEquals(id, buildRequest.getId());
        assertEquals(now, buildRequest.getTimeStamp());
        assertEquals(repository, buildRequest.getRepository());
        assertEquals(ref, buildRequest.getRef());
        assertEquals(commit, buildRequest.getCommit());
        assertEquals(author, buildRequest.getAuthor());
    }
}
