package com.evolvedbinary.rocksdb.cb.orchestrator;

import com.evolvedbinary.rocksdb.cb.dataobject.*;
import com.evolvedbinary.rocksdb.cb.jms.AbstractJMSService;
import com.evolvedbinary.rocksdb.cb.jms.JMSServiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jms.*;
import javax.jms.Queue;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Orchestrator extends AbstractJMSService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Orchestrator.class);
    private static final AtomicReference<JMSServiceState> STATE = new AtomicReference<>(JMSServiceState.IDLE);

    private final Settings settings;
    private final WebHookQueueMessageListener webHookQueueMessageListener = new WebHookQueueMessageListener();
    private final BuildResponseQueueMessageListener buildResponseQueueMessageListener = new BuildResponseQueueMessageListener();

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

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected AtomicReference<JMSServiceState> getState() {
        return STATE;
    }

    @Override
    protected String getClientId() {
        return "orchestrator";
    }

    @Override
    protected List<String> getQueueNames() {
        return Arrays.asList(
                settings.webHookQueueName,
                settings.buildRequestQueueName,
                settings.buildResponseQueueName,
                settings.outputQueueName
        );
    }

    @Nullable
    @Override
    protected MessageListener getListener(final String queueName) {
        if (settings.webHookQueueName.equals(queueName)) {
            return webHookQueueMessageListener;

        } else if (settings.buildResponseQueueName.equals(queueName)) {
            return buildResponseQueueMessageListener;
        }

        return null;
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
        final Queue buildRequestQueue = getQueue(settings.buildRequestQueueName);
        sendMessage(buildRequest, buildRequestQueue);

        // record the updated state from `REQUESTING` to `REQUESTED`
        updateBuildState(builds, buildRequest, BuildState.REQUESTING, BuildState.REQUESTED);
    }

    private void sendOutput(final BuildResponse buildResponse) throws IOException, JMSException {
        // send the message
        final Queue outputQueue = getQueue(settings.outputQueueName);
        sendMessage(buildResponse, outputQueue);
    }

    private class BuildResponseQueueMessageListener implements MessageListener {
        @Override
        public void onMessage(final Message message) {
            if (!(message instanceof TextMessage)) {
                // acknowledge invalid message so that it is removed from the queue
                if (Orchestrator.this.acknowledgeMessage(message)) {
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

            // does the response indicate an update to the state of the build which is OK, or that the build completed, or encountered an error?
            if (BuildState.isStateUpdateSuccessState(buildResponse.getBuildState())) {
                // record the updated success state from `prev` to `next`
                updateBuildState(builds, buildResponse.getBuildRequest(), buildResponse.getBuildState().getPrevBuildState(), buildResponse.getBuildState());

            } else if (BuildState.isStateFinalSuccessState(buildResponse.getBuildState())
                    || BuildState.isStateFailureState(buildResponse.getBuildState())) {

                // record the final state, i.e. DONE!
                if (!removeBuildState(builds, buildResponse.getBuildRequest(), buildResponse.getBuildState().getPrevBuildState())) {
                    LOGGER.error("Unable to remove Build State {} for ref: {} id: {}", buildResponse.getBuildState().getPrevBuildState(), buildResponse.getBuildRequest().getRef(), buildResponse.getBuildRequest().getId());
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
