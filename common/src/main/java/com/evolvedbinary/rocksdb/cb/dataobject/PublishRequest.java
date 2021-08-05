package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishRequest extends AbstractIdentifiableDataObject {

    private BuildResponse buildResponse;

    public PublishRequest() {
        super();
    }

    public PublishRequest(final BuildResponse buildResponse) {
        super();
        this.buildResponse = buildResponse;
    }

    public PublishRequest(final UUID id, final ZonedDateTime timeStamp, final BuildResponse buildResponse) {
        super(id, timeStamp);
        this.buildResponse = buildResponse;
    }

    public BuildResponse getBuildResponse() {
        return buildResponse;
    }

    @Override
    void serializeFields(final JsonGenerator generator) throws IOException {
        generator.writeStringField("id", id.toString());
        generator.writeStringField("timeStamp", timeStamp.toString());
        generator.writeObjectFieldStart("buildResponse");
        buildResponse.serializeFields(generator);
        generator.writeEndObject();
    }

    @Override
    PublishRequest deserializeFields(final JsonParser parser, JsonToken token) throws IOException {
        // new data fields
        String id1 = null;
        String timeStamp1 = null;
        BuildResponse buildResponse1 = null;

        while (true) {
            token = parser.nextToken();
            if (token == null || token == JsonToken.END_OBJECT) {
                break;  // EOL
            }
            if (token == JsonToken.START_OBJECT) {
                throw new IOException("Unexpected Start object: " + token);
            }

            if (token == JsonToken.FIELD_NAME) {
                final String fieldName = parser.getCurrentName();

                // move to field value
                token = parser.nextToken();

                if (token == JsonToken.VALUE_STRING) {
                    if (fieldName.equals("id")) {
                        if (token != JsonToken.VALUE_STRING) {
                            throw new IOException("Expected field string value, but found: " + token);
                        }
                        id1 = parser.getValueAsString();

                    } else if (fieldName.equals("timeStamp")) {
                        if (token != JsonToken.VALUE_STRING) {
                            throw new IOException("Expected field string value, but found: " + token);
                        }
                        timeStamp1 = parser.getValueAsString();

                    }

                } else if (token == JsonToken.START_OBJECT) {
                    if (fieldName.equals("buildResponse")) {
                        buildResponse1 = new BuildResponse().deserializeFields(parser, token);
                    }

                } else {
                    throw new IOException("Expected field string value or start object, but found: " + token);
                }
            }
        }

        if (id1 == null) {
            throw new IOException("Expected id field");
        }
        if (timeStamp1 == null) {
            throw new IOException("Expected timeStamp field");
        }
        if (buildResponse1 == null) {
            throw new IOException("Expected buildResponse field");
        }

        this.id = UUID.fromString(id1);
        this.timeStamp = ZonedDateTime.parse(timeStamp1);
        this.buildResponse = buildResponse1;

        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PublishRequest that = (PublishRequest) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (timeStamp != null ? !timeStamp.equals(that.timeStamp) : that.timeStamp != null) return false;
        return buildResponse.equals(that.buildResponse);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (timeStamp != null ? timeStamp.hashCode() : 0);
        result = 31 * result + buildResponse.hashCode();
        return result;
    }
}
