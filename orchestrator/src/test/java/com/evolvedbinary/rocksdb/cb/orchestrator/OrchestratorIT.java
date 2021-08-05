package com.evolvedbinary.rocksdb.cb.orchestrator;

import com.evolvedbinary.rocksdb.cb.dataobject.*;
import com.evolvedbinary.rocksdb.cb.jms.JMSServiceInstance;
import com.evolvedbinary.rocksdb.cb.junit.JUnit5ExternalResourceAdapter;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.junit.EmbeddedJMSResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.jms.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class OrchestratorIT {

    private static final String WEB_HOOK_QUEUE_NAME = "TestWebHookQueue";
    private static final String BUILD_REQUEST_QUEUE_NAME = "TestBuildRequestQueue";
    private static final String BUILD_RESPONSE_QUEUE_NAME = "TestBuildResponseQueue";
    private static final String PUBLISH_REQUEST_QUEUE_NAME = "TestPublishRequestQueue";
    private static final String PUBLISH_RESPONSE_QUEUE_NAME = "TestPublishResponseQueue";

    private static final int IMMEDIATE_TIMEOUT = -1;
    private static final int MESSAGE_RECEIVE_TIMEOUT = 1000;  // 1 second

    @RegisterExtension @Order(1)
    final JUnit5ExternalResourceAdapter<EmbeddedJMSResource> embeddedJmsExtension = new JUnit5ExternalResourceAdapter<>(new EmbeddedJMSResource(true));

    private Connection connection;
    private Session session;
    private Queue webHookQueue;
    private Queue buildRequestQueue;
    private Queue buildResponseQueue;
    private Queue publishRequestQueue;
    private MessageProducer producer;
    private MessageConsumer buildRequestQueueConsumer;
    private MessageConsumer publishRequestQueueConsumer;

    @BeforeEach
    public void setup() throws JMSException {
        final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        this.connection = connectionFactory.createConnection();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.webHookQueue = session.createQueue(WEB_HOOK_QUEUE_NAME);
        this.buildRequestQueue = session.createQueue(BUILD_REQUEST_QUEUE_NAME);
        this.buildResponseQueue = session.createQueue(BUILD_RESPONSE_QUEUE_NAME);
        this.publishRequestQueue = session.createQueue(PUBLISH_REQUEST_QUEUE_NAME);

        this.producer = session.createProducer(null);
        this.buildRequestQueueConsumer = session.createConsumer(buildRequestQueue);
        this.publishRequestQueueConsumer = session.createConsumer(publishRequestQueue);

        connection.start();
    }

    @AfterEach
    public void tearDown() throws JMSException {
        if (connection != null) {
            connection.stop();
        }

        if (publishRequestQueueConsumer != null) {
            publishRequestQueueConsumer.close();
        }

        if (buildRequestQueueConsumer != null) {
            buildRequestQueueConsumer.close();
        }

        if (producer != null) {
            producer.close();
        }

        if (session != null) {
            session.close();
        }

        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void fromHookToPublishRequestNoRefFiltersAllBuilds() throws IOException, JMSException {
        final Orchestrator.Settings settings = new Orchestrator.Settings(WEB_HOOK_QUEUE_NAME, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, PUBLISH_REQUEST_QUEUE_NAME, PUBLISH_RESPONSE_QUEUE_NAME, Collections.emptyList(), true);
        final Orchestrator orchestrator = new Orchestrator(settings);

        final JMSServiceInstance instance = orchestrator.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);
            message = publishRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            // send a WebHookPayloadSummary to the WebHookQueue
            final WebHookPayloadSummary webHookPayloadSummary = new WebHookPayloadSummary("origin/refs/master", "abc", "def", "facebook/rocksdb", "pusher", "sender");
            message = session.createTextMessage(webHookPayloadSummary.serialize());
            producer.send(webHookQueue, message);

            /*
                Orchestrator should receive and process the WebHookPayloadSummary,
                and then create and send a BuildRequest.
            */

            // expect a BuildRequest on the BuildRequestQueue
            message = buildRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final BuildRequest buildRequest = new BuildRequest().deserialize(((TextMessage)message).getText());
            assertNotNull(buildRequest.getId());
            assertNotNull(buildRequest.getTimeStamp());
            assertEquals(webHookPayloadSummary.getRepository(), buildRequest.getRepository());
            assertEquals(webHookPayloadSummary.getAfter(), buildRequest.getCommit());
            assertEquals(webHookPayloadSummary.getSender(), buildRequest.getAuthor());

            // Send the sequence of update messages that are produced during a successful build to the BuildResponseQueue
            final List<BuildState> updateBuildStates = Arrays.asList(
                    BuildState.UPDATING_SOURCE, BuildState.UPDATING_SOURCE_COMPLETE,
                    BuildState.BUILDING, BuildState.BUILDING_COMPLETE,
                    BuildState.BENCHMARKING);
            for (final BuildState updateBuildState : updateBuildStates) {
                final BuildResponse buildResponse = new BuildResponse(updateBuildState, buildRequest);
                message = session.createTextMessage(buildResponse.serialize());
                producer.send(buildResponseQueue, message);

                // Check output queue is empty, as the orchestrator does not need to response to these it just updates its internal state
                message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
                assertNull(message);
            }

            // Send a BuildResponse(BENCHMARKING_COMPLETE) to the BuildResponseQueue
            final BuildResponse buildResponseBenchmarkingComplete = new BuildResponse(BuildState.BENCHMARKING_COMPLETE, buildRequest);
            message = session.createTextMessage(buildResponseBenchmarkingComplete.serialize());
            producer.send(buildResponseQueue, message);

            /*
                Orchestrator should receive and process the BuildResponse(BENCHMARKING_COMPLETE),
                and update its internal state for the build, and then send the output
                to the OutputQueue
            */

            // expect a PublishRequest on the PublishRequestQueue
            message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final PublishRequest publishRequest = new PublishRequest().deserialize(((TextMessage)message).getText());
            assertEquals(buildResponseBenchmarkingComplete, publishRequest.getBuildResponse());

        } finally {
            instance.close();
        }
    }

    @Test
    public void fromHookToPublishRequestNoRefFiltersAllBuildsUpdatingSourceFailed() throws IOException, JMSException {
        final Orchestrator.Settings settings = new Orchestrator.Settings(WEB_HOOK_QUEUE_NAME, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, PUBLISH_REQUEST_QUEUE_NAME, PUBLISH_RESPONSE_QUEUE_NAME, Collections.emptyList(), true);
        final Orchestrator orchestrator = new Orchestrator(settings);

        final JMSServiceInstance instance = orchestrator.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);
            message = publishRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            // send a WebHookPayloadSummary to the WebHookQueue
            final WebHookPayloadSummary webHookPayloadSummary = new WebHookPayloadSummary("origin/refs/master", "abc", "def", "facebook/rocksdb", "pusher", "sender");
            message = session.createTextMessage(webHookPayloadSummary.serialize());
            producer.send(webHookQueue, message);

            /*
                Orchestrator should receive and process the WebHookPayloadSummary,
                and then create and send a BuildRequest.
            */

            // expect a BuildRequest on the BuildRequestQueue
            message = buildRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final BuildRequest buildRequest = new BuildRequest().deserialize(((TextMessage)message).getText());
            assertNotNull(buildRequest.getId());
            assertNotNull(buildRequest.getTimeStamp());
            assertEquals(webHookPayloadSummary.getRepository(), buildRequest.getRepository());
            assertEquals(webHookPayloadSummary.getAfter(), buildRequest.getCommit());
            assertEquals(webHookPayloadSummary.getSender(), buildRequest.getAuthor());

            // Send a BuildResponse(UPDATING_SOURCE) to the BuildResponseQueue
            final BuildResponse buildResponseBuilding = new BuildResponse(BuildState.UPDATING_SOURCE, buildRequest);
            message = session.createTextMessage(buildResponseBuilding.serialize());
            producer.send(buildResponseQueue, message);

            /*
                Orchestrator should receive and process the BuildResponse(UPDATING_SOURCE),
                and update its internal state for the build.
            */

            // Check output queue is empty
            message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNull(message);

            // Send a BuildResponse(UPDATING_SOURCE_FAILED) to the BuildResponseQueue
            final BuildStats buildStats = new BuildStats();
            final BuildResponse buildResponseUpdatingSourceFailed = new BuildResponse(BuildState.UPDATING_SOURCE_FAILED, buildRequest, buildStats, null);
            message = session.createTextMessage(buildResponseUpdatingSourceFailed.serialize());
            producer.send(buildResponseQueue, message);

            /*
                Orchestrator should receive and process the BuildResponse(UPDATING_SOURCE_FAILED),
                update its internal state for the build, and then send the output
                to the PublishRequestQueue
            */

            // expect a PublishRequest on the PublishRequestQueue
            message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final PublishRequest publishRequest = new PublishRequest().deserialize(((TextMessage)message).getText());
            assertEquals(buildResponseUpdatingSourceFailed, publishRequest.getBuildResponse());

        } finally {
            instance.close();
        }
    }

    @Test
    public void fromHookToPublishRequestNoRefFiltersAllBuildsBuildingFailed() throws IOException, JMSException {
        final Orchestrator.Settings settings = new Orchestrator.Settings(WEB_HOOK_QUEUE_NAME, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, PUBLISH_REQUEST_QUEUE_NAME, PUBLISH_RESPONSE_QUEUE_NAME, Collections.emptyList(), true);
        final Orchestrator orchestrator = new Orchestrator(settings);

        final JMSServiceInstance instance = orchestrator.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);
            message = publishRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            // send a WebHookPayloadSummary to the WebHookQueue
            final WebHookPayloadSummary webHookPayloadSummary = new WebHookPayloadSummary("origin/refs/master", "abc", "def", "facebook/rocksdb", "pusher", "sender");
            message = session.createTextMessage(webHookPayloadSummary.serialize());
            producer.send(webHookQueue, message);

            /*
                Orchestrator should receive and process the WebHookPayloadSummary,
                and then create and send a BuildRequest.
            */

            // expect a BuildRequest on the BuildRequestQueue
            message = buildRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final BuildRequest buildRequest = new BuildRequest().deserialize(((TextMessage)message).getText());
            assertNotNull(buildRequest.getId());
            assertNotNull(buildRequest.getTimeStamp());
            assertEquals(webHookPayloadSummary.getRepository(), buildRequest.getRepository());
            assertEquals(webHookPayloadSummary.getAfter(), buildRequest.getCommit());
            assertEquals(webHookPayloadSummary.getSender(), buildRequest.getAuthor());

            // Send the sequence of update messages that are produced before a BUILDING_FAILED state
            final List<BuildState> updateBuildStates = Arrays.asList(
                    BuildState.UPDATING_SOURCE, BuildState.UPDATING_SOURCE_COMPLETE,
                    BuildState.BUILDING);
            for (final BuildState updateBuildState : updateBuildStates) {
                final BuildResponse buildResponse = new BuildResponse(updateBuildState, buildRequest);
                message = session.createTextMessage(buildResponse.serialize());
                producer.send(buildResponseQueue, message);

                // Check output queue is empty, as the orchestrator does not need to response to these it just updates its internal state
                message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
                assertNull(message);
            }

            // Send a BuildResponse(BUILDING_FAILED) to the BuildResponseQueue
            final BuildStats buildStats = new BuildStats(100, -1, -1);
            final BuildResponse buildResponseBuildingFailed = new BuildResponse(BuildState.BUILDING_FAILED, buildRequest, buildStats, null);
            message = session.createTextMessage(buildResponseBuildingFailed.serialize());
            producer.send(buildResponseQueue, message);

            /*
                Orchestrator should receive and process the BuildResponse(BUILDING_FAILED),
                update its internal state for the build, and then send the output
                to the PublishRequestQueue
            */

            // expect a PublishRequest on the PublishRequestQueue
            message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final PublishRequest publishRequest = new PublishRequest().deserialize(((TextMessage)message).getText());
            assertEquals(buildResponseBuildingFailed, publishRequest.getBuildResponse());

        } finally {
            instance.close();
        }
    }

    @Test
    public void fromHookToPublishRequestNoRefFiltersAllBuildsBenchmarkingFailed() throws IOException, JMSException {
        final Orchestrator.Settings settings = new Orchestrator.Settings(WEB_HOOK_QUEUE_NAME, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, PUBLISH_REQUEST_QUEUE_NAME, PUBLISH_RESPONSE_QUEUE_NAME, Collections.emptyList(), true);
        final Orchestrator orchestrator = new Orchestrator(settings);

        final JMSServiceInstance instance = orchestrator.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);
            message = publishRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            // send a WebHookPayloadSummary to the WebHookQueue
            final WebHookPayloadSummary webHookPayloadSummary = new WebHookPayloadSummary("origin/refs/master", "abc", "def", "facebook/rocksdb", "pusher", "sender");
            message = session.createTextMessage(webHookPayloadSummary.serialize());
            producer.send(webHookQueue, message);

            /*
                Orchestrator should receive and process the WebHookPayloadSummary,
                and then create and send a BuildRequest.
            */

            // expect a BuildRequest on the BuildRequestQueue
            message = buildRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final BuildRequest buildRequest = new BuildRequest().deserialize(((TextMessage)message).getText());
            assertNotNull(buildRequest.getId());
            assertNotNull(buildRequest.getTimeStamp());
            assertEquals(webHookPayloadSummary.getRepository(), buildRequest.getRepository());
            assertEquals(webHookPayloadSummary.getAfter(), buildRequest.getCommit());
            assertEquals(webHookPayloadSummary.getSender(), buildRequest.getAuthor());

            // Send the sequence of update messages that are produced before a BENCHMARKING_FAILED state
            final List<BuildState> updateBuildStates = Arrays.asList(
                    BuildState.UPDATING_SOURCE, BuildState.UPDATING_SOURCE_COMPLETE,
                    BuildState.BUILDING, BuildState.BUILDING_COMPLETE,
                    BuildState.BENCHMARKING);
            for (final BuildState updateBuildState : updateBuildStates) {
                final BuildResponse buildResponse = new BuildResponse(updateBuildState, buildRequest);
                message = session.createTextMessage(buildResponse.serialize());
                producer.send(buildResponseQueue, message);

                // Check output queue is empty, as the orchestrator does not need to response to these it just updates its internal state
                message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
                assertNull(message);
            }

            // Send a BuildResponse(BENCHMARKING_FAILED) to the BuildResponseQueue
            final BuildStats buildStats = new BuildStats(100, 1000, -1);
            final BuildResponse buildResponseBenchmarkingFailed = new BuildResponse(BuildState.BENCHMARKING_FAILED, buildRequest, buildStats, null);
            message = session.createTextMessage(buildResponseBenchmarkingFailed.serialize());
            producer.send(buildResponseQueue, message);

            /*
                Orchestrator should receive and process the BuildResponse(BENCHMARKING_FAILED),
                update its internal state for the build, and then send the output
                to the PublishRequestQueue
            */

            // expect a PublishRequest on the PublishRequestQueue
            message = publishRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final PublishRequest publishRequest = new PublishRequest().deserialize(((TextMessage)message).getText());
            assertEquals(buildResponseBenchmarkingFailed, publishRequest.getBuildResponse());

        } finally {
            instance.close();
        }
    }
}
