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

public class PublishRequestTest {
    @Test
    public void serializeStream() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();
        final UUID publishRequestId = UUID.randomUUID();
        final ZonedDateTime publishRequestTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + publishRequestId.toString() + "\",\"timeStamp\":\"" + publishRequestTimeStamp.toString() + "\",\"buildResponse\":{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}}";

        final BuildRequest deserializedBuildRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse deserializedBuildResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, deserializedBuildRequest, deserializedBuildStats, null);
        final PublishRequest deserializedPublishRequest = new PublishRequest(publishRequestId, publishRequestTimeStamp, deserializedBuildResponse);
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserializedPublishRequest.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeString() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID buildResponseId = UUID.randomUUID();
        final ZonedDateTime buildResponseTimeStamp = ZonedDateTime.now();
        final UUID publishRequestId = UUID.randomUUID();
        final ZonedDateTime publishRequestTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + publishRequestId.toString() + "\",\"timeStamp\":\"" + publishRequestTimeStamp.toString() + "\",\"buildResponse\":{\"id\":\"" + buildResponseId.toString() + "\",\"timeStamp\":\"" + buildResponseTimeStamp.toString() + "\",\"buildState\":\"BUILDING\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"},\"buildStats\":{\"updateSourceTime\":1000,\"compilationTime\":2000,\"benchmarkTime\":3000}}}";

        final BuildRequest deserializedBuildRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final BuildStats deserializedBuildStats = new BuildStats(1000, 2000, 3000);
        final BuildResponse deserializedBuildResponse = new BuildResponse(buildResponseId, buildResponseTimeStamp, BuildState.BUILDING, deserializedBuildRequest, deserializedBuildStats, null);
        final PublishRequest deserializedPublishRequest = new PublishRequest(publishRequestId, publishRequestTimeStamp, deserializedBuildResponse);
        final String serialized = deserializedPublishRequest.serialize();
        assertEquals(expected, serialized);
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
