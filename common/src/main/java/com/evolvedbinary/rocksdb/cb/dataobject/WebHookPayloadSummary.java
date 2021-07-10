package com.evolvedbinary.rocksdb.cb.dataobject;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.UUID;

public class WebHookPayloadSummary extends AbstractIdentifiableDataObject {

    private String ref;
    private String before;
    private String after;
    private String repository;
    private String pusher;
    private String sender;

    public WebHookPayloadSummary() {
        super();
    }

    public WebHookPayloadSummary(final String ref, final String before, final String after, final String repository, final String pusher, final String sender) {
        super();
        this.ref = ref;
        this.before = before;
        this.after = after;
        this.repository = repository;
        this.pusher = pusher;
        this.sender = sender;
    }

    public WebHookPayloadSummary(final UUID id, final ZonedDateTime timeStamp, final String ref, final String before, final String after, final String repository, final String pusher, final String sender) {
        super(id, timeStamp);
        this.ref = ref;
        this.before = before;
        this.after = after;
        this.repository = repository;
        this.pusher = pusher;
        this.sender = sender;
    }

    public String getRef() {
        return ref;
    }

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    public String getRepository() {
        return repository;
    }

    public String getPusher() {
        return pusher;
    }

    public String getSender() {
        return sender;
    }

    @Override
    public void serializeFields(final JsonGenerator generator) throws IOException {
        generator.writeStringField("id", id.toString());
        generator.writeStringField("timeStamp", timeStamp.toString());

        generator.writeStringField("ref", ref);
        generator.writeStringField("before", before);
        generator.writeStringField("after", after);
        generator.writeStringField("repository", repository);
        generator.writeStringField("pusher", pusher);
        generator.writeStringField("sender", sender);
    }

    @Override
    public WebHookPayloadSummary deserializeFields(final JsonParser parser, JsonToken token) throws IOException {
        // new data fields
        String id1 = null;
        String timeStamp1 = null;
        String ref1 = null;
        String before1 = null;
        String after1 = null;
        String repository1 = null;
        String pusher1 = null;
        String sender1 = null;

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
                } else if (fieldName.equals("ref")) {
                    ref1 = parser.getValueAsString();
                } else if (fieldName.equals("before")) {
                    before1 = parser.getValueAsString();
                } else if (fieldName.equals("after")) {
                    after1 = parser.getValueAsString();
                } else if (fieldName.equals("repository")) {
                    repository1 = parser.getValueAsString();
               } else if (fieldName.equals("pusher")) {
                    pusher1 = parser.getValueAsString();
               } else if (fieldName.equals("sender")) {
                    sender1 = parser.getValueAsString();
               }
            }
        }

        this.id = UUID.fromString(id1);
        this.timeStamp = ZonedDateTime.parse(timeStamp1);
        this.ref = ref1;
        this.before = before1;
        this.after = after1;
        this.repository = repository1;
        this.pusher = pusher1;
        this.sender = sender1;

        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final WebHookPayloadSummary that = (WebHookPayloadSummary) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (timeStamp != null ? !timeStamp.equals(that.timeStamp) : that.timeStamp != null) return false;
        if (ref != null ? !ref.equals(that.ref) : that.ref != null) return false;
        if (before != null ? !before.equals(that.before) : that.before != null) return false;
        if (after != null ? !after.equals(that.after) : that.after != null) return false;
        if (repository != null ? !repository.equals(that.repository) : that.repository != null) return false;
        if (pusher != null ? !pusher.equals(that.pusher) : that.pusher != null) return false;
        return sender != null ? sender.equals(that.sender) : that.sender == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (timeStamp != null ? timeStamp.hashCode() : 0);
        result = 31 * result + (ref != null ? ref.hashCode() : 0);
        result = 31 * result + (before != null ? before.hashCode() : 0);
        result = 31 * result + (after != null ? after.hashCode() : 0);
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (pusher != null ? pusher.hashCode() : 0);
        result = 31 * result + (sender != null ? sender.hashCode() : 0);
        return result;
    }
}
