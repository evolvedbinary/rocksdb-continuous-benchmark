package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.dataobject.WebHookPayloadSummary;
import org.junit.jupiter.api.Test;
import com.evolvedbinary.rocksdb.cb.github.WebHookPayloadParser.InvalidJsonException;
import com.evolvedbinary.rocksdb.cb.github.WebHookPayloadParser.InvalidPayloadException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebHookPayloadParserTest {

    @Test
    public void nullInput() {
        final WebHookPayloadParser parser = new WebHookPayloadParser();
        assertThrows(InvalidJsonException.class, () -> {
            parser.parse(null);
        });
    }

    @Test
    public void emptyString() {
        final WebHookPayloadParser parser = new WebHookPayloadParser();
        assertThrows(InvalidJsonException.class, () -> {
            parser.parse("");
        });
    }

    @Test
    public void notJson() {
        final WebHookPayloadParser parser = new WebHookPayloadParser();
        assertThrows(InvalidJsonException.class, () -> {
            parser.parse("<this-is-not-json/>");
        });
    }

    @Test
    public void invalidJson() {
        final WebHookPayloadParser parser = new WebHookPayloadParser();
        assertThrows(InvalidJsonException.class, () -> {
            parser.parse("{ \"field\": value }");
        });
    }

    @Test
    public void invalidPayload() {
        final WebHookPayloadParser parser = new WebHookPayloadParser();
        assertThrows(InvalidPayloadException.class, () -> {
            parser.parse("{\"key\" : \"value\"}");
        });
    }

    @Test
    public void incompleteMinPushPayload() throws IOException {
        final String inputJson = readTestJson("incomplete-min-push-payload-sample.json");
        final WebHookPayloadParser parser = new WebHookPayloadParser();

        assertThrows(InvalidPayloadException.class, () -> {
            parser.parse(inputJson);
        });
    }

    @Test
    public void completeMinPushPayload() throws IOException, InvalidJsonException, InvalidPayloadException {
        final String inputJson = readTestJson("complete-min-push-payload-sample.json");

        final WebHookPayloadParser parser = new WebHookPayloadParser();
        final WebHookPayloadSummary payload = parser.parse(inputJson);

        assertEquals("refs/heads/master", payload.getRef());
        assertEquals("6113728f27ae82c7b1a177c8d03f9e96e0adf246", payload.getBefore());
        assertEquals("0000000000000000000000000000000000000000", payload.getAfter());
        assertEquals("Codertocat/Hello-World", payload.getRepository());
        assertEquals("Codertocat", payload.getPusher());
        assertEquals("Other", payload.getSender());
    }

    @Test
    public void fullPushPayload() throws IOException, InvalidJsonException, InvalidPayloadException {
        final String inputJson = readTestJson("push-payload-sample.json");

        final WebHookPayloadParser parser = new WebHookPayloadParser();
        final WebHookPayloadSummary payload = parser.parse(inputJson);

        assertEquals("refs/tags/simple-tag", payload.getRef());
        assertEquals("6113728f27ae82c7b1a177c8d03f9e96e0adf246", payload.getBefore());
        assertEquals("0000000000000000000000000000000000000000", payload.getAfter());
        assertEquals("Codertocat/Hello-World", payload.getRepository());
        assertEquals("Codertocat", payload.getPusher());
        assertEquals("Codertocat", payload.getSender());
    }

    private String readTestJson(final String filename) throws IOException {
        final byte[] buf = new byte[1024];
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (final InputStream isJson = getClass().getResourceAsStream(filename)) {
            int read = -1;
            while ((read = isJson.read(buf)) != -1) {
                os.write(buf, 0, read);
            }
        }
        return os.toString(UTF_8);
    }
}
