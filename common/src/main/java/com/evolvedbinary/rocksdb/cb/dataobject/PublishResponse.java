package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishResponse extends AbstractIdentifiableDataObject {

    private PublishState publishState;
    private BuildRequest buildRequest;

    public PublishResponse() {
        super();
    }

    public PublishResponse(final PublishState publishState, final BuildRequest buildRequest) {
        super();
        this.publishState = publishState;
        this.buildRequest = buildRequest;
    }

    public PublishResponse(final UUID id, final ZonedDateTime timeStamp, final PublishState publishState, final BuildRequest buildRequest) {
        super(id, timeStamp);
        this.publishState = publishState;
        this.buildRequest = buildRequest;
    }

    public PublishState getPublishState() {
        return publishState;
    }

    public BuildRequest getBuildRequest() {
        return buildRequest;
    }

    @Override
    void serializeFields(final JsonGenerator generator) throws IOException {
        generator.writeStringField("id", id.toString());
        generator.writeStringField("timeStamp", timeStamp.toString());
        generator.writeStringField("publishState", publishState.name());
        generator.writeObjectFieldStart("buildRequest");
        buildRequest.serializeFields(generator);
        generator.writeEndObject();
    }

    @Override
    PublishResponse deserializeFields(final JsonParser parser, JsonToken token) throws IOException {
        // new data fields
        String id1 = null;
        String timeStamp1 = null;
        PublishState publishState1 = null;
        BuildRequest buildRequest1 = null;

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

                    } else if (fieldName.equals("publishState")) {
                        final String fieldValue = parser.getValueAsString();
                        try {
                            publishState1 = PublishState.valueOf(fieldValue);
                        } catch (final IllegalArgumentException e) {
                            throw new IOException("Expected field publishState to have a valid PublishState, but found: " + fieldValue);
                        }
                    }

                } else if (token == JsonToken.START_OBJECT) {
                    if (fieldName.equals("buildRequest")) {
                        buildRequest1 = new BuildRequest().deserializeFields(parser, token);
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
        if (publishState1 == null) {
            throw new IOException("Expected publishState field");
        }
        if (buildRequest1 == null) {
            throw new IOException("Expected buildRequest field");
        }

        this.id = UUID.fromString(id1);
        this.timeStamp = ZonedDateTime.parse(timeStamp1);
        this.publishState = publishState1;
        this.buildRequest = buildRequest1;

        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PublishResponse that = (PublishResponse) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (timeStamp != null ? !timeStamp.equals(that.timeStamp) : that.timeStamp != null) return false;
        if (publishState != that.publishState) return false;
        return buildRequest.equals(buildRequest);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (timeStamp != null ? timeStamp.hashCode() : 0);
        result = 31 * result + publishState.hashCode();
        result = 31 * result + buildRequest.hashCode();
        return result;
    }
}
