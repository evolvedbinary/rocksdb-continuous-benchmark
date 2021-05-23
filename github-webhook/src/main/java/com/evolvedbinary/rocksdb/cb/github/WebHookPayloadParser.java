package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.dataobject.WebHookPayloadSummary;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class WebHookPayloadParser {
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private JsonParser parser = null;
    private JsonToken token = null;
    private String prevFieldName;

    /**
     * Parse a GitHub Webhook Payload.
     *
     * @param json the Webhook payload.
     *
     * @return the payload summary
     *
     * @throws InvalidJsonException if the JSON is not well-formed
     * @throws InvalidPayloadException if the JSON is not a valid GitHub Webhook Payload
     */
    public WebHookPayloadSummary parse(final String json) throws InvalidJsonException, InvalidPayloadException {
        if (json == null) {
            throw new InvalidJsonException("Input is null");
        } else if (json.isEmpty()) {
            throw new InvalidJsonException("Input is empty");
        }

        try {
            this.parser = JSON_FACTORY.createParser(json);

            // get the first token
            token = parser.nextToken();

            // Sanity check: verify that we got "Json Object":
            if (token != JsonToken.START_OBJECT) {
                throw new InvalidPayloadException("Expected data to start with an Object");
            }

            final PayloadCapturedFields payloadCapturedFields = new PayloadCapturedFields();

            // Iterate until the end, or exit
            final Deque<String> parents = new ArrayDeque<>();
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    prevFieldName = parser.getCurrentName();
                } else if (token == JsonToken.START_OBJECT) {
                    parents.push(prevFieldName);
                } else if (token == JsonToken.END_OBJECT) {
                    prevFieldName = parents.poll();
                }

                if (parents.isEmpty()) {
                    if (payloadCapturedFields.ref == null) {
                        payloadCapturedFields.ref = ifFieldGetString("ref");
                    }
                    if (payloadCapturedFields.before == null) {
                        payloadCapturedFields.before = ifFieldGetString("before");
                    }
                    if (payloadCapturedFields.after == null) {
                        payloadCapturedFields.after = ifFieldGetString("after");
                    }
                } else if (parents.size() == 1) {
                    final String parent = parents.peek();
                    if ("repository".equals(parent) && payloadCapturedFields.repository == null) {
                        payloadCapturedFields.repository = ifFieldGetString("full_name");
                    } else if ("pusher".equals(parent) && payloadCapturedFields.pusher == null) {
                        payloadCapturedFields.pusher = ifFieldGetString("name");
                    } else if ("sender".equals(parent) && payloadCapturedFields.sender == null) {
                        payloadCapturedFields.sender = ifFieldGetString("login");
                    }
                }

                if (payloadCapturedFields.isComplete()) {
                    return new WebHookPayloadSummary(payloadCapturedFields.ref, payloadCapturedFields.before, payloadCapturedFields.after, payloadCapturedFields.repository, payloadCapturedFields.pusher, payloadCapturedFields.sender);
                }
            }
        } catch (final IOException e) {
            throw new InvalidJsonException(e);
        } finally {
            if (parser != null) {
                try {
                    parser.close();
                } catch (final IOException e) {
                    throw new InvalidJsonException(e);
                } finally {
                    parser = null;
                    token = null;
                }
            }
        }

        throw new InvalidPayloadException("Could not find required fields in payload");
    }

    private @Nullable String ifFieldGetString(final String fieldName) throws IOException {
        return ifField(fieldName, this::getFieldStringValue);
    }

    private @Nullable String getFieldStringValue() throws IOException {
        if (token == JsonToken.VALUE_STRING) {
            return parser.getText();
        }
        return null;
    }

    private @Nullable <T> T ifField(final String fieldName, final ValueExtractor<T> valueExtractor) throws IOException {
        if (token == JsonToken.FIELD_NAME) {
            if (fieldName.equals(prevFieldName)) {
                token = parser.nextToken();
                return valueExtractor.extract();
            }
        }
        return null;
    }

    static class InvalidJsonException extends Exception {
        public InvalidJsonException(final String message) {
            super(message);
        }

        public InvalidJsonException(final Throwable cause) {
            super(cause);
        }
    }

    static class InvalidPayloadException extends Exception {
        public InvalidPayloadException(final String message) {
            super(message);
        }
    }

    @FunctionalInterface
    private interface ValueExtractor<T> {
        @Nullable T extract() throws IOException;
    }

    private static class PayloadCapturedFields {
        String ref = null;
        String before = null;
        String after = null;
        String repository = null;
        String pusher = null;
        String sender = null;

        public boolean isComplete() {
            return
                    (ref != null && !ref.isEmpty())
                    && (before != null && !before.isEmpty())
                    && (after != null && !after.isEmpty())
                    && (repository != null && !repository.isEmpty())
                    && (pusher != null && !pusher.isEmpty())
                    && (sender != null && !sender.isEmpty());
        }
    }
}
