package com.evolvedbinary.rocksdb.cb.github;

import net.jcip.annotations.GuardedBy;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.*;

public class ServerResource implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback {

    private static final int MIN_RANDOM_PORT = 49152;
    private static final int MAX_RANDOM_PORT = 65535;
    private static final int MAX_RANDOM_PORT_ATTEMPTS = 10;
    @GuardedBy("class") private static final Random random = new Random();
    private final WebHookPayloadSummaryProcessor webHookPayloadSummaryProcessor;

    /**
     * Ensures that before/after is only
     * called once if beforeAll and afterAll
     * are in play, e.g. this resource
     * is static resource.
     */
    private int referenceCount;
    private int port = -1;
    private Server.Instance instance;

    public ServerResource(final WebHookPayloadSummaryProcessor webHookPayloadSummaryProcessor) {
        this.webHookPayloadSummaryProcessor = webHookPayloadSummaryProcessor;
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        before(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        before(extensionContext);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        after(extensionContext);
    }


    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        after(extensionContext);
    }

    private void before(final ExtensionContext extensionContext) throws InterruptedException, URISyntaxException {
        if (++referenceCount != 1) {
            return;
        }

        this.port = nextFreePort(MIN_RANDOM_PORT, MAX_RANDOM_PORT);
        final Server.Settings settings = new Server.Settings(TestSSLCertificate.getKeyStorePath(), Optional.of(TestSSLCertificate.getKeyStorePass()), Optional.of(TestSSLCertificate.getCertPass()), port);

        final Server server = new Server(settings, webHookPayloadSummaryProcessor);
        this.instance = server.runAsync();
    }

    private void after(final ExtensionContext extensionContext) {
        if (--referenceCount != 0) {
            return;
        }

        instance.close();
        instance = null;
        this.port = -1;
    }

    public int nextFreePort(final int from, final int to) {
        for (int attempts = 0; attempts < MAX_RANDOM_PORT_ATTEMPTS; attempts++) {
            final int port = random(from, to);
            if (isLocalPortFree(port)) {
                return port;
            }
        }

        throw new IllegalStateException("Exceeded MAX_RANDOM_PORT_ATTEMPTS");
    }

    private synchronized int random(final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    private boolean isLocalPortFree(final int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    public int getPort() {
        return port;
    }
}