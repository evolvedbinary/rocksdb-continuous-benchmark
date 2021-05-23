package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.UUID;

public class BuildRequest extends AbstractIdentifiableDataObject {

    private String repository;
    private String ref;
    private String commit;
    private String author;

    public BuildRequest() {
        super();
    }

    public BuildRequest(final UUID id, final ZonedDateTime timeStamp, final String repository, final String ref, final String commit, final String author) {
        super(id, timeStamp);
        this.repository = repository;
        this.ref = ref;
        this.commit = commit;
        this.author = author;
    }

    public BuildRequest(final String repository, final String ref, final String commit, final String author) {
        super();
        this.repository = repository;
        this.ref = ref;
        this.commit = commit;
        this.author = author;
    }

    public String getRepository() {
        return repository;
    }

    public String getRef() {
        return ref;
    }

    public String getCommit() {
        return commit;
    }

    public String getAuthor() {
        return author;
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
        generator.writeStringField("id", id.toString());
        generator.writeStringField("timeStamp", timeStamp.toString());

        generator.writeStringField("repository", repository);
        generator.writeStringField("ref", ref);
        generator.writeStringField("commit", commit);
        generator.writeStringField("author", author);
    }

    @Override
    public BuildRequest deserialize(final InputStream is) throws IOException {
        try (final JsonParser parser = JSON_FACTORY.createParser(is)) {
            // get the first token
            final JsonToken token = parser.nextToken();
            return deserialize(parser, token);
        }
    }

    BuildRequest deserialize(final JsonParser parser, JsonToken token) throws IOException {
        // Sanity check: verify that we got "Json Object":
        if (token != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
        }

        // new data fields
        String id1 = null;
        String timeStamp1 = null;
        String repository1 = null;
        String ref1 = null;
        String commit1 = null;
        String author1 = null;

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
                if (token != JsonToken.VALUE_STRING) {
                    throw new IOException("Expected field string value, but found: " + token);
                }

                if (fieldName.equals("id")) {
                    id1 = parser.getValueAsString();
                } else if (fieldName.equals("timeStamp")) {
                    timeStamp1 = parser.getValueAsString();
                } else if (fieldName.equals("repository")) {
                    repository1 = parser.getValueAsString();
                } else if (fieldName.equals("ref")) {
                    ref1 = parser.getValueAsString();
                } else if (fieldName.equals("commit")) {
                    commit1 = parser.getValueAsString();
                } else if (fieldName.equals("author")) {
                    author1 = parser.getValueAsString();
                }
            }
        }

        if (id1 == null) {
            throw new IOException("Expected id field");
        }
        if (timeStamp1 == null) {
            throw new IOException("Expected timeStamp field");
        }
        if (repository1 == null) {
            throw new IOException("Expected repository field");
        }
        if (ref1 == null) {
            throw new IOException("Expected ref field");
        }
        if (commit1 == null) {
            throw new IOException("Expected commit field");
        }
        if (author1 == null) {
            throw new IOException("Expected author field");
        }

        this.id = UUID.fromString(id1);
        this.timeStamp = ZonedDateTime.parse(timeStamp1);
        this.repository = repository1;
        this.ref = ref1;
        this.commit = commit1;
        this.author = author1;

        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final BuildRequest that = (BuildRequest) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (timeStamp != null ? !timeStamp.equals(that.timeStamp) : that.timeStamp != null) return false;
        if (repository != null ? !repository.equals(that.repository) : that.repository != null) return false;
        if (ref != null ? !ref.equals(that.ref) : that.ref != null) return false;
        if (commit != null ? !commit.equals(that.commit) : that.commit != null) return false;
        return author != null ? author.equals(that.author) : that.author == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (timeStamp != null ? timeStamp.hashCode() : 0);
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (ref != null ? ref.hashCode() : 0);
        result = 31 * result + (commit != null ? commit.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        return result;
    }
}
