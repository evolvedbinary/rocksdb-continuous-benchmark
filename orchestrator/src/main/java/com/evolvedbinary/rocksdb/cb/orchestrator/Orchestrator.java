package com.evolvedbinary.rocksdb.cb.orchestrator;

import com.evolvedbinary.rocksdb.cb.dataobject.*;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jms.*;
import javax.jms.Queue;
import java.io.Closeable;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.evolvedbinary.rocksdb.cb.common.CloseUtil.closeAndLogIfException;

public class Orchestrator {

    private enum State {
        IDLE,
        RUNNING,
        AWAITING_SHUTDOWN,
        SHUTTING_DOWN
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Orchestrator.class);
    private static final AtomicReference<State> STATE = new AtomicReference<>(State.IDLE);

    private final Settings settings;

    private Connection connection;
    private Session session;
    private Queue webHookQueue;
    private Queue buildRequestQueue;
    private Queue buildResponseQueue;
    private Queue outputQueue;
    private MessageProducer producer;
    private MessageConsumer webHookQueueConsumer;
    private MessageConsumer buildResponseQueueConsumer;

    // TODO(AR) internal state needs to be persisted somewhere -- load and resume after restart
    private final Map<String, Map<UUID, Build>> builds = new ConcurrentHashMap<>();
    private final Map<String, BuildRequest> buildBacklog = new ConcurrentHashMap<>();

    private static class Build {
        final BuildRequest request;
        final BuildState state;

        public Build(final BuildRequest request, final BuildState state) {
            this.request = request;
            this.state = state;
        }
    }

    public Orchestrator(final Settings settings) {
        this.settings = settings;
    }

    public void runSync() throws InterruptedException {
        final Instance instance = runAsync();
        instance.awaitShutdown();
    }

    public Instance runAsync() {
        if (!STATE.compareAndSet(State.IDLE, State.RUNNING)) {
            throw new IllegalStateException("Already running");
        }

        // setup JMS
        final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        try {
            this.connection = connectionFactory.createConnection();
            this.connection.setClientID("orchestrator");

            this.session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            this.webHookQueue = session.createQueue(settings.webHookQueueName);
            this.buildRequestQueue = session.createQueue(settings.buildRequestQueueName);
            this.buildResponseQueue = session.createQueue(settings.buildResponseQueueName);
            this.outputQueue = session.createQueue(settings.outputQueueName);

            this.producer = session.createProducer(null);

            this.webHookQueueConsumer = session.createConsumer(webHookQueue);
            this.webHookQueueConsumer.setMessageListener(new WebHookQueueMessageListener());
            LOGGER.info("Listening to Queue: {}", settings.webHookQueueName);

            this.buildResponseQueueConsumer = session.createConsumer(buildResponseQueue);
            this.buildResponseQueueConsumer.setMessageListener(new BuildResponseQueueMessageListener());
            LOGGER.info("Listening to Queue: {}", settings.buildResponseQueueName);

            // start the connection
            this.connection.start();

        } catch (final JMSException e) {
            if (this.connection != null) {
                closeAndLogIfException(connection::stop, LOGGER);
            }

            closeAndLogIfException(this.buildResponseQueueConsumer, LOGGER);
            closeAndLogIfException(this.webHookQueueConsumer, LOGGER);
            closeAndLogIfException(this.producer, LOGGER);
            closeAndLogIfException(this.session, LOGGER);
            closeAndLogIfException(this.connection, LOGGER);

            throw new RuntimeException("Unable to setup JMS broker connection: " + e.getMessage(), e);
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(1, r -> new Thread(r, "Orchestrator-Thread"));
        final Future<?> orchestrateFuture = executorService.submit(new OrchestratorCallable());

        return new Instance(executorService, orchestrateFuture);
    }

    private class WebHookQueueMessageListener implements MessageListener {
        @Override
        public void onMessage(final Message message) {
            if (!(message instanceof TextMessage)) {
                // acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected type {} from Queue: {}.", message.getClass().getName(), settings.webHookQueueName);
                }

                // can't process non-text message, so DONE
                return;
            }

            final TextMessage textMessage = (TextMessage) message;
            final String content;
            try {
                content = textMessage.getText();
            } catch (final JMSException e) {
                LOGGER.error("Could not get content of TextMessage from Queue: {}. Error: {}", settings.webHookQueueName, e.getMessage(), e);

                // can't access message content, so DONE
                return;
            }

            // attempt to parse as WebHookPayloadSummary
            final WebHookPayloadSummary webHookPayloadSummary;
            try {
                webHookPayloadSummary = new WebHookPayloadSummary().deserialize(content);
            } catch (final IOException e) {
                // unable to deserialize, acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected format from Queue: {}. Error: {}. Content: '{}'", settings.webHookQueueName, e.getMessage(), content);
                }
                return;
            }

            // filter out any refs that we don't want to process
            if (!matchesRefs(webHookPayloadSummary)) {
                // we should discard this message
                // acknowledge discarded message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.debug("WebHookPayloadSummary in Queue: {} does not match ref-pattern(s), discarded message with ref: {}", settings.webHookQueueName, webHookPayloadSummary.getRef());
                }
                return;
            }

            final BuildRequest buildRequest = new BuildRequest(webHookPayloadSummary.getRepository(), webHookPayloadSummary.getRef(), webHookPayloadSummary.getAfter(), webHookPayloadSummary.getSender());
            if (processBuildRequest(buildRequest)) {
                // acknowledge processed message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.info("Processed WebHookPayloadSummary(id={}) from Queue: {}.", webHookPayloadSummary.getId(), settings.webHookQueueName);
                }
            }
        }

        private boolean matchesRefs(final WebHookPayloadSummary webHookPayloadSummary) {
            if (settings.refPatterns.isEmpty()) {
                return true;
            }

            for (final Pattern refPattern : settings.refPatterns) {
                final Matcher refMatcher = refPattern.matcher(webHookPayloadSummary.getRef());
                if (refMatcher.matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean processBuildRequest(final BuildRequest buildRequest) {
        // should we create builds for all requests that come in?
        if (settings.allBuilds) {
            // always send the build request...

            // record that we are requesting a build
            insertBuildState(builds, buildRequest, BuildState.REQUESTING);

            // send it...
            try {
                sendBuildRequest(buildRequest);
            } catch (final IOException | JMSException e) {
                LOGGER.error("Unable to send BuildRequest to Queue: {}. Error: ", settings.buildRequestQueueName, e.getMessage(), e);
                return false;  // DONE - can't process message, so don't acknowledge it!
            }

        } else {
            // only build when there isn't a build happening for the ref

            // is there a build request in progress, if not record that we are requesting a build
            final boolean noExistingBuildForRef = insertBuildStateIfAbsent(builds, buildRequest, BuildState.REQUESTING);

            if (noExistingBuildForRef) {
                // this is a the first build request for this ref, so send it...
                try {
                    sendBuildRequest(buildRequest);
                } catch (final IOException | JMSException e) {
                    LOGGER.error("Unable to send BuildRequest to Queue: {}. Error: ", settings.buildRequestQueueName, e.getMessage(), e);
                    return false;  // DONE - can't process message, so don't acknowledge it!
                }

            } else {
                // there is an existing build request for this ref, so place this one on the backlog (if it is newer)
                buildBacklog.compute(buildRequest.getRef(), (k,v) -> {
                    if (v == null) {
                        return buildRequest;
                    }

                    // if the buildRequest has a newer timestamp, then update v
                    if (v.getTimeStamp().isBefore(buildRequest.getTimeStamp())) {
                        v = buildRequest;
                    }

                    return v;
                });
            }
        }

        return true;
    }

    private void sendBuildRequest(final BuildRequest buildRequest) throws IOException, JMSException {
        // send the message
        sendMessage(buildRequest, buildRequestQueue);

        // record the updated state from `REQUESTING` to `REQUESTED`
        updateBuildState(builds, buildRequest, BuildState.REQUESTING, BuildState.REQUESTED);
    }

    private void sendOutput(final BuildResponse buildResponse) throws IOException, JMSException {
        // send the message
        sendMessage(buildResponse, outputQueue);
    }

    private void sendMessage(final DataObject message, final Queue queue) throws IOException, JMSException {
        // send the message
        final String content = message.serialize();
        final TextMessage textMessage = session.createTextMessage(content);
        producer.send(queue, textMessage);
        LOGGER.info("Sent {} to Queue: {}", message.getClass().getName(), queue.getQueueName());
    }

    private class BuildResponseQueueMessageListener implements MessageListener {
        @Override
        public void onMessage(final Message message) {
            if (!(message instanceof TextMessage)) {
                // acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected type {} from Queue: {}.", message.getClass().getName(), settings.buildResponseQueueName);
                }

                // can't process non-text message, so DONE
                return;
            }

            final TextMessage textMessage = (TextMessage) message;
            final String content;
            try {
                content = textMessage.getText();
            } catch (final JMSException e) {
                LOGGER.error("Could not get content of TextMessage from Queue: {}. Error: {}", settings.buildResponseQueueName, e.getMessage(), e);

                // can't access message content, so DONE
                return;
            }

            // attempt to parse as BuildResponse
            final BuildResponse buildResponse;
            try {
                buildResponse = new BuildResponse().deserialize(content);
            } catch (final IOException e) {
                // unable to deserialize, acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected format from Queue: {}. Error: {}. Content: '{}'", settings.buildResponseQueueName, e.getMessage(), content);
                }
                return;
            }

            // does response indicate building or built?
            if (BuildState.BUILDING == buildResponse.getBuildState()) {
                // record the updated state from `REQUESTED` to `BUILDING`
                updateBuildState(builds, buildResponse.getBuildRequest(), BuildState.REQUESTED, BuildState.BUILDING);

            } else if (BuildState.BUILT == buildResponse.getBuildState()) {

                // record the updated state from `BUILDING` to `BUILT`, i.e. DONE!
                if (!removeBuildState(builds, buildResponse.getBuildRequest(), BuildState.BUILDING)) {
                    LOGGER.error("Unable to remove Build State for ref: {} id: {}", buildResponse.getBuildRequest().getRef(), buildResponse.getBuildRequest().getId());
                }

                // is there a build in the backlog? if so dispatch it
                final BuildRequest backloggedBuildRequest = buildBacklog.remove(buildResponse.getBuildRequest().getRef());
                if (backloggedBuildRequest != null) {
                    processBuildRequest(backloggedBuildRequest);
                }

                // TODO(AR) improve data sent to output queue

                // dispatch the results to the output queue
                try {
                    sendOutput(buildResponse);
                } catch (final IOException | JMSException e) {
                    LOGGER.error("Unable to send BuildResponse to Queue: {}. Error: ", settings.outputQueueName, e.getMessage(), e);
                }
            }

            acknowledgeMessage(message);
        }
    }

    private static boolean acknowledgeMessage(final Message message) {
        try {
            message.acknowledge();
            return true;
        } catch (final JMSException e) {
            LOGGER.error("Unable to acknowledge message: {}", e.getMessage(), e);
            return false;
        }
    }

    static boolean insertBuildState(final Map<String, Map<UUID, Build>> builds, final BuildRequest buildRequest, final BuildState insertState) {
        final Map<UUID, Build> existingBuildsForRef = builds.compute(buildRequest.getRef(), (k, v) -> {
            if (v == null) {
                v = new HashMap<>();
            }

            final Build existingBuild = v.get(buildRequest.getId());
            if (existingBuild != null) {
                if (insertState == existingBuild.state) {
                    LOGGER.warn("Found existing build for ref: {} with id: {} and same expected state: {}. Ignoring...", buildRequest.getRef(), buildRequest.getId(), existingBuild.state.name());
                    return v;
                } else {
                    LOGGER.error("Found existing build for ref: {} with id: {} but state: {} != insert state: {}. Ignoring...", buildRequest.getRef(), buildRequest.getId(), existingBuild.state.name(), insertState.name());
                    return v;
                }
            }

            v.put(buildRequest.getId(), new Build(buildRequest, insertState));
            return v;
        });

        //TODO(AR) below... get() is performed on Map existingBuildsForRef which can be mutated by another thread accessing builds Map

        // were we able to insert the build for the ref
        final Build existingBuild = existingBuildsForRef.get(buildRequest.getId());
        final boolean inserted = existingBuild != null && existingBuild.state == insertState;
        if (inserted) {
            LOGGER.trace("Inserted Build State for ref: {} id: {}, {}", buildRequest.getRef(), buildRequest.getId(), insertState.name());
        }
        return inserted;
    }

    static boolean insertBuildStateIfAbsent(final Map<String, Map<UUID, Build>> builds, final BuildRequest buildRequest, final BuildState insertState) {
        final Map<UUID, Build> existingBuildsForRef = builds.computeIfAbsent(buildRequest.getRef(), k -> {
            final Map<UUID, Build> v = new HashMap<>();
            v.put(buildRequest.getId(), new Build(buildRequest, insertState));
            return v;
        });

        //TODO(AR) below... containsKey() is performed on Map existingBuildsForRef which can be mutated by another thread accessing builds Map

        // were we able to insert the build for the ref
        final boolean inserted = existingBuildsForRef.containsKey(buildRequest.getId());
        if (inserted) {
            LOGGER.trace("Inserted Build State for ref: {} id: {}, {}", buildRequest.getRef(), buildRequest.getId(), insertState.name());
        }
        return inserted;
    }

    static boolean updateBuildState(final Map<String, Map<UUID, Build>> builds, final BuildRequest buildRequest, final BuildState fromState, final BuildState toState) {
        // record the updated state fromState -> toState
        final Map<UUID, Build> existingBuildsForRef = builds.compute(buildRequest.getRef(), (k, v) -> {
            if (v == null) {
                LOGGER.warn("Expecting at least 1 build for ref: {} in {} state, but found null. Will create with state: {}", buildRequest.getRef(), fromState.name(), toState.name());
                v = new HashMap<>();
                v.put(buildRequest.getId(), new Build(buildRequest, toState));
                return v;
            }

            final Build existingBuild = v.get(buildRequest.getId());
            if (existingBuild == null) {
                LOGGER.warn("Expecting build for ref: {} with id: {} not found. Will create with state: {}", buildRequest.getRef(), buildRequest.getId(), toState.name());
                v.put(buildRequest.getId(), new Build(buildRequest, toState));
                return v;
            }

            if (fromState == existingBuild.state) {
                v.put(buildRequest.getId(), new Build(existingBuild.request, toState));
            } else {
                LOGGER.error("Expected build for ref: {} with id: {} to be in {} state, but was in {} state. Ignoring...", buildRequest.getRef(), buildRequest.getId(), fromState.name(), existingBuild.state.name());
            }
            return v;
        });

        //TODO(AR) below... get() is performed on Map existingBuildsForRef which can be mutated by another thread accessing builds Map

        // were we able to insert the build for the ref
        final Build existingBuild = existingBuildsForRef.get(buildRequest.getId());
        final boolean updated = existingBuild != null && existingBuild.state == toState;
        if (updated) {
            LOGGER.trace("Update Build State for ref: {} id: {}, {} -> {}", buildRequest.getRef(), buildRequest.getId(), fromState.name(), toState.name());
        }
        return updated;
    }

    static boolean removeBuildState(final Map<String, Map<UUID, Build>> builds, final BuildRequest buildRequest, final BuildState removeState) {
        @Nullable final Map<UUID, Build> existingBuildsForRef = builds.compute(buildRequest.getRef(), (k, v) -> {
            if (v == null) {
                return v;
            }

            final Build existingBuild = v.get(buildRequest.getId());
            if (existingBuild != null) {
                if (removeState != existingBuild.state) {
                    LOGGER.error("Found existing build for ref: {} with id: {} but state: {} != remove state: {}. Ignoring...", buildRequest.getRef(), buildRequest.getId(), existingBuild.state.name(), removeState.name());
                    return v;
                }
            }

            v.remove(buildRequest.getId());
            return v;
        });

        //TODO(AR) below... containsKey() is performed on Map existingBuildsForRef which can be mutated by another thread accessing builds Map

        // were we able to insert the build for the ref
        final boolean removed = existingBuildsForRef == null || !existingBuildsForRef.containsKey(buildRequest.getId());
        if (removed) {
            LOGGER.trace("Removed Build State for ref: {} id: {}, {}", buildRequest.getRef(), buildRequest.getId(), removeState.name());
        }
        return removed;
    }

    private class OrchestratorCallable implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            try {

                // loop and sleep... until InterruptedException
                while (true) {
                    Thread.sleep(5000);
                }

            } catch (final Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();  // restore interrupt flag
                }

                // attempt JMS shutdown
                if (connection != null) {
                    closeAndLogIfException(connection::stop, LOGGER);
                }
                closeAndLogIfException(buildResponseQueueConsumer, LOGGER);
                closeAndLogIfException(webHookQueueConsumer, LOGGER);
                closeAndLogIfException(producer, LOGGER);
                closeAndLogIfException(session, LOGGER);
                closeAndLogIfException(connection, LOGGER);

                throw e;
            }
        }
    }

    static class Instance implements Closeable {
        private final ExecutorService executorService;
        private final Future<?> orchestrateFuture;

        private Instance(final ExecutorService executorService, final Future<?> orchestrateFuture) {
            this.executorService = executorService;
            this.orchestrateFuture = orchestrateFuture;
        }

        /**
         * Wait until the orchestrate future completes.
         */
        public void awaitShutdown() throws InterruptedException {
            if (!STATE.compareAndSet(State.RUNNING, State.AWAITING_SHUTDOWN)) {
                throw new IllegalStateException("Not running");
            }

            try {
                orchestrateFuture.get();

                if (!executorService.isShutdown()) {
                    executorService.shutdownNow();
                }

            } catch (final ExecutionException e) {
                LOGGER.error("Orchestrator raised an exception: " + e.getMessage(), e);
            } finally {
                STATE.set(State.IDLE);
            }
        }

        @Override
        public void close() {
            if (!STATE.compareAndSet(State.RUNNING, State.SHUTTING_DOWN)) {
                throw new IllegalStateException("Not running");
            }

            try {
                orchestrateFuture.cancel(true);

                if (!executorService.isShutdown()) {
                    executorService.shutdownNow();
                }

            } finally {
                STATE.set(State.IDLE);
            }
        }
    }

    static class Settings {
        final String webHookQueueName;
        final String buildRequestQueueName;
        final String buildResponseQueueName;
        final String outputQueueName;
        final List<Pattern> refPatterns;
        final boolean allBuilds;

        public Settings(final String webHookQueueName, final String buildRequestQueueName, final String buildResponseQueueName, final String outputQueueName, final List<Pattern> refPatterns, final boolean allBuilds) {
            this.webHookQueueName = webHookQueueName;
            this.buildRequestQueueName = buildRequestQueueName;
            this.buildResponseQueueName = buildResponseQueueName;
            this.outputQueueName = outputQueueName;
            this.refPatterns = refPatterns;
            this.allBuilds = allBuilds;
        }
    }
}
