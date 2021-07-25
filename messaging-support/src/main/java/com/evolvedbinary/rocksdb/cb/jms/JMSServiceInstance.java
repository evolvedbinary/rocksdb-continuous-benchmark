package com.evolvedbinary.rocksdb.cb.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class JMSServiceInstance implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSServiceInstance.class);

    private final ExecutorService executorService;
    private final String jmsClientId;
    private final AtomicReference<JMSServiceState> jmsServiceState;
    private final Future<?> jmsServiceFuture;

    JMSServiceInstance(final ExecutorService executorService, final String jmsClientId, final AtomicReference<JMSServiceState> jmsServiceState, final Future<?> jmsServiceFuture) {
        this.executorService = executorService;
        this.jmsClientId = jmsClientId;
        this.jmsServiceState = jmsServiceState;
        this.jmsServiceFuture = jmsServiceFuture;
    }

    /**
     * Wait until the JMSService future completes.
     */
    public void awaitShutdown() throws InterruptedException {
        if (!jmsServiceState.compareAndSet(JMSServiceState.RUNNING, JMSServiceState.AWAITING_SHUTDOWN)) {
            throw new IllegalStateException("Not running");
        }

        try {
            jmsServiceFuture.get();

            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }

        } catch (final ExecutionException e) {
            LOGGER.error("JMSServiceInstance for ClientID: " + jmsClientId + " raised an exception: " + e.getMessage(), e);
        } finally {
            jmsServiceState.set(JMSServiceState.IDLE);
        }
    }

    @Override
    public void close() {
        if (!jmsServiceState.compareAndSet(JMSServiceState.RUNNING, JMSServiceState.SHUTTING_DOWN)) {
            throw new IllegalStateException("Not running");
        }

        try {
            jmsServiceFuture.cancel(true);

            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }

        } finally {
            jmsServiceState.set(JMSServiceState.IDLE);
        }
    }
}
