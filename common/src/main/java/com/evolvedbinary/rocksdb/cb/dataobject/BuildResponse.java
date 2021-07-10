package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuildResponse extends AbstractIdentifiableDataObject {

    private BuildState buildState;
    private BuildRequest buildRequest;
    @Nullable private BuildStats buildStats;
    @Nullable private List<BuildDetail> buildDetails;

    public BuildResponse() {
       super();
    }

    public BuildResponse(final BuildState buildState, final BuildRequest buildRequest) {
        this(buildState, buildRequest, null, null);
    }

    public BuildResponse(final BuildState buildState, final BuildRequest buildRequest, @Nullable final BuildStats buildStats, @Nullable final List<BuildDetail> buildDetails) {
        super();
        this.buildState = buildState;
        this.buildRequest = buildRequest;
        this.buildStats = buildStats;
        this.buildDetails = buildDetails;
    }

    public BuildResponse(final UUID id, final ZonedDateTime timeStamp, final BuildState buildState, final BuildRequest buildRequest) {
        this(id, timeStamp, buildState, buildRequest, null, null);
    }

    public BuildResponse(final UUID id, final ZonedDateTime timeStamp, final BuildState buildState, final BuildRequest buildRequest, @Nullable final BuildStats buildStats, @Nullable final List<BuildDetail> buildDetails) {
        super(id, timeStamp);
        this.buildState = buildState;
        this.buildRequest = buildRequest;
        this.buildStats = buildStats;
        this.buildDetails = buildDetails;
    }

    public BuildState getBuildState() {
        return buildState;
    }

    public BuildRequest getBuildRequest() {
        return buildRequest;
    }

    public @Nullable BuildStats getBuildStats() {
        return buildStats;
    }

    @Override
    void serializeFields(final JsonGenerator generator) throws IOException {
        generator.writeStringField("id", id.toString());
        generator.writeStringField("timeStamp", timeStamp.toString());
        generator.writeStringField("buildState", buildState.name());
        generator.writeObjectFieldStart("buildRequest");
        buildRequest.serializeFields(generator);
        generator.writeEndObject();
        if (buildStats != null) {
            generator.writeObjectFieldStart("buildStats");
            buildStats.serializeFields(generator);
            generator.writeEndObject();
        }
        if (buildDetails != null && !buildDetails.isEmpty()) {
            generator.writeArrayFieldStart("buildDetails");

            for (final BuildDetail buildDetail : buildDetails) {
                generator.writeStartObject();
                buildDetail.serializeFields(generator);
                generator.writeEndObject();
            }

            generator.writeEndArray();
        }
    }

    @Override
    public BuildResponse deserializeFields(final JsonParser parser, JsonToken token) throws IOException {

        // new data fields
        String id1 = null;
        String timeStamp1 = null;
        BuildState buildState1 = null;
        BuildRequest buildRequest1 = null;
        BuildStats buildStats1 = null;
        List<BuildDetail> buildDetails1 = null;

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

                    } else if (fieldName.equals("buildState")) {
                        final String fieldValue = parser.getValueAsString();
                        try {
                            buildState1 = BuildState.valueOf(fieldValue);
                        } catch (final IllegalArgumentException e) {
                            throw new IOException("Expected field buildState to have a valid BuildState, but found: " + fieldValue);
                        }
                    }

                } else if (token == JsonToken.START_OBJECT) {
                    if (fieldName.equals("buildRequest")) {
                        buildRequest1 = new BuildRequest().deserializeFields(parser, token);

                    } else if (fieldName.equals("buildStats")) {
                        buildStats1 = new BuildStats().deserializeFields(parser, token);
                    }

                } else if (token == JsonToken.START_ARRAY) {
                    if (fieldName.equals("buildDetails")) {
                        buildDetails1 = new ArrayList<>();
                        while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
                            if (token == JsonToken.START_OBJECT) {
                                final BuildDetail buildDetail1 = new BuildDetail().deserializeFields(parser, token);
                                buildDetails1.add(buildDetail1);
                            }
                        }
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
        if (buildState1 == null) {
            throw new IOException("Expected buildState field");
        }
        if (buildRequest1 == null) {
            throw new IOException("Expected buildRequest field");
        }

        this.id = UUID.fromString(id1);
        this.timeStamp = ZonedDateTime.parse(timeStamp1);
        this.buildState = buildState1;
        this.buildRequest = buildRequest1;
        this.buildStats = buildStats1;
        this.buildDetails = (buildDetails1 != null && !buildDetails1.isEmpty()) ? buildDetails1 : null;

        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final BuildResponse that = (BuildResponse) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (timeStamp != null ? !timeStamp.equals(that.timeStamp) : that.timeStamp != null) return false;
        if (buildState != that.buildState) return false;
        if (buildRequest != null ? !buildRequest.equals(that.buildRequest) : that.buildRequest != null) return false;
        if (buildStats != null ? !buildStats.equals(that.buildStats) : that.buildStats != null) return false;
        return buildDetails != null ? buildDetails.equals(that.buildDetails) : that.buildDetails == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (timeStamp != null ? timeStamp.hashCode() : 0);
        result = 31 * result + (buildState != null ? buildState.hashCode() : 0);
        result = 31 * result + (buildRequest != null ? buildRequest.hashCode() : 0);
        result = 31 * result + (buildStats != null ? buildStats.hashCode() : 0);
        result = 31 * result + (buildDetails != null ? buildDetails.hashCode() : 0);
        return result;
    }
}
