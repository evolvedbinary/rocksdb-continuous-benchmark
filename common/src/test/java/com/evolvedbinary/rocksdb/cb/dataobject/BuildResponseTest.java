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

public class BuildResponseTest {

    @Test
    public void serializeStreamWithoutStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, deserializedRequest);
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserializedResponse.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStreamWithStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILT\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"cloneTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILT, deserializedRequest, deserializedBuildStats);
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserializedResponse.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringWthoutStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, deserializedRequest);
        final String serialized = deserializedResponse.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringWithStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILT\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"cloneTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILT, deserializedRequest, deserializedBuildStats);
        final String serialized = deserializedResponse.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void deserializeStreamWithoutStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, expectedRequest);

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}}";
        final BuildResponse deserialized = new BuildResponse();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStreamWithStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats expectedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILT, expectedRequest, expectedBuildStats);

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILT\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"cloneTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";
        final BuildResponse deserialized = new BuildResponse();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStringWithoutStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, expectedRequest);

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}}";
        final BuildResponse deserialized = new BuildResponse().deserialize(serialized);
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStringWithStats() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats expectedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILT, expectedRequest, expectedBuildStats);

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILT\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"cloneTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";
        final BuildResponse deserialized = new BuildResponse().deserialize(serialized);
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeNonJson() {
        assertThrows(IOException.class, () -> {
            new BuildResponse().deserialize("<this-is-not-json/>");
        });
    }

    @Test
    public void deserializeJsonArray() {
        assertThrows(IOException.class, () -> {
            new BuildResponse().deserialize("[]");
        });
    }

    @Test
    public void deserializeUnexpectedJson() {
        final UUID id = UUID.randomUUID();
        assertThrows(IOException.class, () -> {
            new BuildResponse().deserialize("{\"id\":\"" + id.toString() + "\",\"other\":{}}");
        });
    }
}
