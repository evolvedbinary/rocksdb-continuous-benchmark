package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.dataobject.WebHookPayloadSummary;
import io.restassured.http.ContentType;
import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServerIT {

    @RegisterExtension
    static final ServerResource SERVER = new ServerResource(new MockWebHookPayloadSummaryProcessor());

    @Test
    public void noGetMethodOnEndpoint() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                get(getEndpointUri()).
        then().
                statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noHeadMethodOnEndpoint() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                head(getEndpointUri()).
        then().
                statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noPutMethodOnEndpoint() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                put(getEndpointUri()).
        then().
                statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noDeleteMethodOnEndpoint() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                delete(getEndpointUri()).
        then().
                statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noGetMethodOnAPI() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                get(getApiUri()).
        then().
                statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noHeadMethodOnAPI() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                head(getApiUri()).
        then().
                statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noPutMethodOnAPI() throws URISyntaxException {
        given()
            .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                put(getApiUri()).
        then().
            statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noDeleteMethodOnAPI() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                 delete(getApiUri()).
        then().
                statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void noPostOnNonHttpsEndpoint() {
        assertThrows(NoHttpResponseException.class, () -> {
            given()
                    .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
            when().
                    post(getEndpointUri().replace("https", "http"));
        });
    }

    @Test
    public void noPostOnNonHttpsApi() {
        assertThrows(NoHttpResponseException.class, () -> {
            given()
                    .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
            when().
                    post(getApiUri().replace("https", "http"));
        });
    }

    @Test
    public void postOnEndpointReturnsNotFound() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                post(getEndpointUri()).
        then().
                statusCode(SC_NOT_FOUND);
    }

    @Test
    public void postOnApiWithoutJsonContentReturnsUnsupportedMediaType() throws URISyntaxException {
        given()
            .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
            post(getApiUri()).
        then().
            statusCode(SC_UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    public void postEmptyJsonToApiReturnsBadRequest() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                contentType(ContentType.JSON).
                post(getApiUri()).
        then().
                statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void postNonJsonToApiReturnsBadRequest() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                contentType(ContentType.JSON).
                body("<this-is-not-json/>").
                post(getApiUri()).
        then().
                statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void postInvalidJsonToApiReturnsBadRequest() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                contentType(ContentType.JSON).
                body("{ \"field\": value }").
                post(getApiUri()).
        then().
                statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void postNonGitHubHookJsonToApiReturnsUnprocessableEntity() throws URISyntaxException {
        given()
                .trustStore(TestSSLCertificate.getTrustStorePath().toString(), TestSSLCertificate.getTrustStorePass()).
        when().
                contentType(ContentType.JSON).
                body("{\"key\" : \"value\"}").
                post(getApiUri()).
        then().
                statusCode(SC_UNPROCESSABLE_ENTITY);
    }

    private String getEndpointUri() {
        return "https://localhost:" + SERVER.getPort();
    }

    private String getApiUri() {
        return getEndpointUri() + "/cb";
    }

    private static class MockWebHookPayloadSummaryProcessor implements WebHookPayloadSummaryProcessor {
        @Override
        public void process(final WebHookPayloadSummary webHookPayloadSummary) throws IOException {
        }
    }
}
