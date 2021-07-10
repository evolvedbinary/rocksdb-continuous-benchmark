package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BuildStats extends AbstractDataObject {

    private long updateSourceTime = -1;
    private long compilationTime = -1;
    private long benchmarkTime = -1;

    public BuildStats() {
    }

    public BuildStats(final long updateSourceTime, final long compilationTime, final long benchmarkTime) {
        this.updateSourceTime = updateSourceTime;
        this.compilationTime = compilationTime;
        this.benchmarkTime = benchmarkTime;
    }

    public long getUpdateSourceTime() {
        return updateSourceTime;
    }

    public void setUpdateSourceTime(final long updateSourceTime) {
        this.updateSourceTime = updateSourceTime;
    }

    public long getCompilationTime() {
        return compilationTime;
    }

    public void setCompilationTime(final long compilationTime) {
        this.compilationTime = compilationTime;
    }

    public long getBenchmarkTime() {
        return benchmarkTime;
    }

    public void setBenchmarkTime(final long benchmarkTime) {
        this.benchmarkTime = benchmarkTime;
    }

    @Override
    public void serialize(final OutputStream os) throws IOException {
        try (final JsonGenerator generator = JSON_FACTORY.createGenerator(os)) {
            generator.writeStartObject();
            serializeFields(generator);
            generator.writeEndObject();
        }
    }

    void serializeFields(final JsonGenerator generator) throws IOException {
        generator.writeNumberField("updateSourceTime", updateSourceTime);
        generator.writeNumberField("compilationTime", compilationTime);
        generator.writeNumberField("benchmarkTime", benchmarkTime);
    }

    @Override
    public BuildStats deserialize(final InputStream is) throws IOException {
        try (final JsonParser parser = JSON_FACTORY.createParser(is)) {
            // get the first token
            final JsonToken token = parser.nextToken();
            return deserialize(parser, token);
        }
    }

    BuildStats deserialize(final JsonParser parser, JsonToken token) throws IOException {
        // Sanity check: verify that we got "Json Object":
        if (token != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
        }

        // new data fields
        long updateSourceTime1 = -1;
        long compilationTime1 = -1;
        long benchmarkTime1 = -1;

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
                if (token != JsonToken.VALUE_NUMBER_INT) {
                    throw new IOException("Expected field int value, but found: " + token);
                }

                if (fieldName.equals("updateSourceTime")) {
                    updateSourceTime1 = parser.getValueAsLong();
                } else if (fieldName.equals("compilationTime")) {
                    compilationTime1 = parser.getValueAsLong();
                } else if (fieldName.equals("benchmarkTime")) {
                    benchmarkTime1 = parser.getValueAsLong();
                }
            }
        }

        this.updateSourceTime = updateSourceTime1;
        this.compilationTime = compilationTime1;
        this.benchmarkTime = benchmarkTime1;

        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final BuildStats that = (BuildStats) o;

        if (updateSourceTime != that.updateSourceTime) return false;
        if (compilationTime != that.compilationTime) return false;
        return benchmarkTime == that.benchmarkTime;
    }

    @Override
    public int hashCode() {
        int result = (int) (updateSourceTime ^ (updateSourceTime >>> 32));
        result = 31 * result + (int) (compilationTime ^ (compilationTime >>> 32));
        result = 31 * result + (int) (benchmarkTime ^ (benchmarkTime >>> 32));
        return result;
    }
}
