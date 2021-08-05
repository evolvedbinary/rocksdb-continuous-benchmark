package com.evolvedbinary.rocksdb.cb.dataobject;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PublishResponseTest {
    @Test
    public void serializeStream() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID publishRequestId = UUID.randomUUID();
        final ZonedDateTime publishRequestTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + publishRequestId.toString() + "\",\"timeStamp\":\"" + publishRequestTimeStamp.toString() + "\",\"publishState\":\"COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}}";

        final BuildRequest deserializedBuildRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final PublishResponse deserializedPublishResponse = new PublishResponse(publishRequestId, publishRequestTimeStamp, PublishState.COMPLETE, deserializedBuildRequest);
        final String serialized;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            deserializedPublishResponse.serialize(baos);
            serialized = baos.toString(UTF_8);
        }
        assertEquals(expected, serialized);
    }

    @Test
    public void serializeString() throws IOException {
        final UUID buildRequestId = UUID.randomUUID();
        final ZonedDateTime buildRequestTimeStamp = ZonedDateTime.now();
        final UUID publishRequestId = UUID.randomUUID();
        final ZonedDateTime publishRequestTimeStamp = ZonedDateTime.now();

        final String expected = "{\"id\":\"" + publishRequestId.toString() + "\",\"timeStamp\":\"" + publishRequestTimeStamp.toString() + "\",\"publishState\":\"COMPLETE\",\"buildRequest\":{\"id\":\"" + buildRequestId.toString() + "\",\"timeStamp\":\"" + buildRequestTimeStamp.toString() + "\",\"repository\":\"facebook/rocksdb\",\"ref\":\"origin/refs/master\",\"commit\":\"abc\",\"author\":\"person1\"}}";

        final BuildRequest deserializedBuildRequest = new BuildRequest(buildRequestId, buildRequestTimeStamp, "facebook/rocksdb", "origin/refs/master", "abc", "person1");
        final PublishResponse deserializedPublishReseponse = new PublishResponse(publishRequestId, publishRequestTimeStamp, PublishState.COMPLETE, deserializedBuildRequest);
        final String serialized = deserializedPublishReseponse.serialize();
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
