package com.evolvedbinary.rocksdb.cb.dataobject;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BuildResponseTest {

    @Test
    public void serializeStreamWithoutStatsWithoutFailure() throws IOException {
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
    public void serializeStreamWithStatsWithoutFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, deserializedRequest, deserializedBuildStats, null);
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserializedResponse.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStreamWithoutStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, deserializedRequest, null, Arrays.asList(BuildDetail.forException(new IOException("message1"))));
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserializedResponse.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStreamWithStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":-1},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, -1);
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, deserializedRequest, deserializedBuildStats, Arrays.asList(BuildDetail.forException(new IOException("message1"))));
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserializedResponse.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringWithoutStatsWithoutFailure() throws IOException {
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
    public void serializeStringWithStatsWithoutFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, deserializedRequest, deserializedBuildStats, null);
        final String serialized = deserializedResponse.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringWithoutStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, deserializedRequest, null, Arrays.asList(BuildDetail.forException(new IOException("message1"))));
        final String serialized = deserializedResponse.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeStringWithStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":-1},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";

        final BuildRequest deserializedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, -1);
        final BuildResponse deserializedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, deserializedRequest, deserializedBuildStats, Arrays.asList(BuildDetail.forException(new IOException("message1"))));
        final String serialized = deserializedResponse.serialize();
        assertEquals(expected, serialized);
    }

    @Test
    public void deserializeStreamWithoutStatsWithoutFailure() throws IOException {
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
    public void deserializeStreamWithStatsWithoutFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats expectedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, expectedRequest, expectedBuildStats, null);

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";
        final BuildResponse deserialized = new BuildResponse();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStreamWithoutStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, expectedRequest, null, Arrays.asList(BuildDetail.forException(new IOException("message1"))));

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";
        final BuildResponse deserialized = new BuildResponse();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStreamWithStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats expectedBuildStats = new BuildStats(1000, 2000, -1);
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, expectedRequest, expectedBuildStats, Arrays.asList(BuildDetail.forException(new IOException("message1"))));

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":-1},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";
        final BuildResponse deserialized = new BuildResponse();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(UTF_8))) {
            deserialized.deserialize(bais);
        }
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStringWithoutStatsWithoutFailure() throws IOException {
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
    public void deserializeStringWithStatsWithoutFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats expectedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, expectedRequest, expectedBuildStats, null);

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}";
        final BuildResponse deserialized = new BuildResponse().deserialize(serialized);
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStringWithoutStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, expectedRequest, null, Arrays.asList(BuildDetail.forException(new IOException("message1"))));

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";
        final BuildResponse deserialized = new BuildResponse().deserialize(serialized);
        assertEquals(expectedResponse, deserialized);
    }

    @Test
    public void deserializeStringWithStatsWithFailure() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();

        final BuildRequest expectedRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats expectedBuildStats = new BuildStats(1000, 2000, -1);
        final BuildResponse expectedResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BENCHMARKING_COMPLETE, expectedRequest, expectedBuildStats, Arrays.asList(BuildDetail.forException(new IOException("message1"))));

        final String serialized = "{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BENCHMARKING_COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":-1},\"buildDetails\":[{\"type\":\"EXCEPTION_MESSAGE\",\"detailCompression\":\"NONE\",\"detailEncoding\":\"NONE\",\"detail\":\"message1\"}]}";
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
