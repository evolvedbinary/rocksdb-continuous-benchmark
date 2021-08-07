package com.evolvedbinary.rocksdb.cb.runner;

import com.evolvedbinary.rocksdb.cb.Constants;
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
import org.junit.jupiter.api.io.TempDir;

import javax.jms.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import static com.evolvedbinary.rocksdb.cb.common.DequeUtil.Deque;
import static com.evolvedbinary.rocksdb.cb.dataobject.BuildState.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class RunnerIT {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");

    private static final String BUILD_REQUEST_QUEUE_NAME = "TestBuildRequestQueue";
    private static final String BUILD_RESPONSE_QUEUE_NAME = "TestBuildResponseQueue";

    private static final int IMMEDIATE_TIMEOUT = -1;

    @RegisterExtension
    @Order(1)
    final JUnit5ExternalResourceAdapter<EmbeddedJMSResource> embeddedJmsExtension = new JUnit5ExternalResourceAdapter<>(new EmbeddedJMSResource(true));

    private Connection connection;
    private Session session;
    private Queue buildRequestQueue;
    private Queue buildResponseQueue;
    private MessageProducer producer;
    private MessageConsumer buildResponseQueueConsumer;

    @BeforeEach
    public void setup() throws JMSException {
        final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        this.connection = connectionFactory.createConnection();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.buildRequestQueue = session.createQueue(BUILD_REQUEST_QUEUE_NAME);
        this.buildResponseQueue = session.createQueue(BUILD_RESPONSE_QUEUE_NAME);

        this.producer = session.createProducer(null);
        this.buildResponseQueueConsumer = session.createConsumer(buildResponseQueue);

        connection.start();
    }

    @AfterEach
    public void tearDown() throws JMSException {
        if (connection != null) {
            connection.stop();
        }

        if (buildResponseQueueConsumer != null) {
            buildResponseQueueConsumer.close();
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
    public void fromBuildRequestToBenchmarkingComplete(@TempDir final Path tempDir) throws IOException, JMSException {
        assumeFalse(IS_WINDOWS);

        final Runner.Settings settings = new Runner.Settings(Constants.DEFAULT_ARTEMIS_HOST, Constants.DEFAULT_ARTEMIS_PORT, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, tempDir, null, null, true, true);
        final Runner runner = new Runner(settings);

        final JMSServiceInstance instance = runner.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            final String repository = "facebook/rocksdb";
            final String ref = "origin/refs/master";
            final String commit = "9dc887e";
            final String author = "adamretter";

            // send a BuildRequest to the BuildRequestQueue
            final BuildRequest buildRequest = new BuildRequest(repository, ref, commit, author);
            message = session.createTextMessage(buildRequest.serialize());
            producer.send(buildRequestQueue, message);

            /*
                Runner should receive and process the BuildRequest,
                and then send the following response messages in sequence:
                  - UPDATING_SOURCE
                  - UPDATING_SOURCE_COMPLETE
                  - BUILDING
                  - BUILDING_COMPLETE
                  - BENCHMARKING
                  - BENCHMARKING_COMPLETE
            */

            final Deque<BuildState> expectedBuildStates = Deque(
                    UPDATING_SOURCE,
                    UPDATING_SOURCE_COMPLETE,
                    BUILDING,
                    BUILDING_COMPLETE,
                    BENCHMARKING,
                    BENCHMARKING_COMPLETE
            );

            BuildState expectedBuildState = null;
            while ((expectedBuildState = expectedBuildStates.poll()) != null) {

                // check that the response is as expected
                message = buildResponseQueueConsumer.receive();
                assertNotNull(message);
                assertTrue(message instanceof TextMessage);

                final BuildResponse buildResponse = new BuildResponse().deserialize(((TextMessage)message).getText());

                assertEquals(expectedBuildState, buildResponse.getBuildState());
                assertEquals(buildRequest, buildResponse.getBuildRequest());

                if(expectedBuildState == BENCHMARKING_COMPLETE) {
                    final List<BuildDetail> buildDetails = buildResponse.getBuildDetails();
                    assertNotNull(buildDetails);
                    assertEquals(1, buildDetails.size());
                    final BuildDetail buildDetail = buildDetails.get(0);
                    assertEquals(BuildDetailType.STDOUT_LOG, buildDetail.getBuildDetailType());
                } else {
                    final List<BuildDetail> buildDetails = buildResponse.getBuildDetails();
                    assertNull(buildDetails);
                }
            }

            // post-flight - Check queues that we consume are now empty
            message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

        } finally {
            instance.close();
        }
    }

    @Test
    public void fromBuildRequestToUpdatingSourceFailed_noSuchRepo(@TempDir final Path tempDir) throws IOException, JMSException {
        final Runner.Settings settings = new Runner.Settings(Constants.DEFAULT_ARTEMIS_HOST, Constants.DEFAULT_ARTEMIS_PORT, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, tempDir, null, null, true, true);
        final Runner runner = new Runner(settings);

        final JMSServiceInstance instance = runner.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            final String repository = "facebook/NOT-rocksdb";
            final String ref = "origin/refs/master";
            final String commit = "9dc887e";
            final String author = "adamretter";

            // send a BuildRequest to the BuildRequestQueue
            final BuildRequest buildRequest = new BuildRequest(repository, ref, commit, author);
            message = session.createTextMessage(buildRequest.serialize());
            producer.send(buildRequestQueue, message);

            /*
                Runner should receive and process the BuildRequest,
                and then send the following response messages in sequence:
                  - UPDATING_SOURCE
                  - UPDATING_SOURCE_FAILED
            */

            final Deque<BuildState> expectedBuildStates = Deque(
                    UPDATING_SOURCE,
                    UPDATING_SOURCE_FAILED
            );

            BuildState expectedBuildState = null;
            while ((expectedBuildState = expectedBuildStates.poll()) != null) {

                // check that the response is as expected
                message = buildResponseQueueConsumer.receive();
                assertNotNull(message);
                assertTrue(message instanceof TextMessage);

                final BuildResponse buildResponse = new BuildResponse().deserialize(((TextMessage)message).getText());

                assertEquals(expectedBuildState, buildResponse.getBuildState());
                assertEquals(buildRequest, buildResponse.getBuildRequest());

                if (expectedBuildState == UPDATING_SOURCE_FAILED) {
                    final List<BuildDetail> buildDetails = buildResponse.getBuildDetails();
                    assertNotNull(buildDetails);
                    assertEquals(1, buildDetails.size());
                    final BuildDetail buildDetail = buildDetails.get(0);
                    assertEquals(BuildDetailType.EXCEPTION_MESSAGE, buildDetail.getBuildDetailType());
                    assertTrue(new String(buildDetail.getDetail(), UTF_8).startsWith("Unable to clone"));
                }
            }

            // post-flight - Check queues that we consume are now empty
            message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

        } finally {
            instance.close();
        }
    }

    @Test
    public void fromBuildRequestToUpdatingSourceFailed_noSuchCommit(@TempDir final Path tempDir) throws IOException, JMSException {
        final Runner.Settings settings = new Runner.Settings(Constants.DEFAULT_ARTEMIS_HOST, Constants.DEFAULT_ARTEMIS_PORT, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, tempDir, null, null, true, true);
        final Runner runner = new Runner(settings);

        final JMSServiceInstance instance = runner.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            final String repository = "facebook/rocksdb";
            final String ref = "origin/refs/master";
            final String commit = "1234567";
            final String author = "adamretter";

            // send a BuildRequest to the BuildRequestQueue
            final BuildRequest buildRequest = new BuildRequest(repository, ref, commit, author);
            message = session.createTextMessage(buildRequest.serialize());
            producer.send(buildRequestQueue, message);

            /*
                Runner should receive and process the BuildRequest,
                and then send the following response messages in sequence:
                  - UPDATING_SOURCE
                  - UPDATING_SOURCE_FAILED
            */

            final Deque<BuildState> expectedBuildStates = Deque(
                    UPDATING_SOURCE,
                    UPDATING_SOURCE_FAILED
            );

            BuildState expectedBuildState = null;
            while ((expectedBuildState = expectedBuildStates.poll()) != null) {

                // check that the response is as expected
                message = buildResponseQueueConsumer.receive();
                assertNotNull(message);
                assertTrue(message instanceof TextMessage);

                final BuildResponse buildResponse = new BuildResponse().deserialize(((TextMessage)message).getText());

                assertEquals(expectedBuildState, buildResponse.getBuildState());
                assertEquals(buildRequest, buildResponse.getBuildRequest());

                if (expectedBuildState == UPDATING_SOURCE_FAILED) {
                    final List<BuildDetail> buildDetails = buildResponse.getBuildDetails();
                    assertNotNull(buildDetails);
                    assertEquals(1, buildDetails.size());
                    final BuildDetail buildDetail = buildDetails.get(0);
                    assertEquals(BuildDetailType.EXCEPTION_MESSAGE, buildDetail.getBuildDetailType());
                    assertEquals("Unable to checkout: " + commit + ". Ref " + commit + " cannot be resolved", new String(buildDetail.getDetail(), UTF_8));
                }
            }

            // post-flight - Check queues that we consume are now empty
            message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

        } finally {
            instance.close();
        }
    }

    @Test
    public void fromBuildRequestToBuildingFailed(@TempDir final Path tempDir) throws IOException, JMSException {
        final String invalidBuildCommand = "no-such-build-command";
        final Runner.Settings settings = new Runner.Settings(Constants.DEFAULT_ARTEMIS_HOST, Constants.DEFAULT_ARTEMIS_PORT, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, tempDir, invalidBuildCommand, null, true, true);
        final Runner runner = new Runner(settings);

        final JMSServiceInstance instance = runner.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            final String repository = "facebook/rocksdb";
            final String ref = "origin/refs/master";
            final String commit = "9dc887e";
            final String author = "adamretter";

            // send a BuildRequest to the BuildRequestQueue
            final BuildRequest buildRequest = new BuildRequest(repository, ref, commit, author);
            message = session.createTextMessage(buildRequest.serialize());
            producer.send(buildRequestQueue, message);

            /*
                Runner should receive and process the BuildRequest,
                and then send the following response messages in sequence:
                  - UPDATING_SOURCE
                  - UPDATING_SOURCE_COMPLETE
                  - BUILDING
                  - BUILDING_FAILED
            */

            final Deque<BuildState> expectedBuildStates = Deque(
                    UPDATING_SOURCE,
                    UPDATING_SOURCE_COMPLETE,
                    BUILDING,
                    BUILDING_FAILED
            );

            BuildState expectedBuildState = null;
            while ((expectedBuildState = expectedBuildStates.poll()) != null) {

                // check that the response is as expected
                message = buildResponseQueueConsumer.receive();
                assertNotNull(message);
                assertTrue(message instanceof TextMessage);

                final BuildResponse buildResponse = new BuildResponse().deserialize(((TextMessage)message).getText());

                assertEquals(expectedBuildState, buildResponse.getBuildState());
                assertEquals(buildRequest, buildResponse.getBuildRequest());

                if (expectedBuildState == BUILDING_FAILED) {
                    final List<BuildDetail> buildDetails = buildResponse.getBuildDetails();
                    assertNotNull(buildDetails);
                    assertEquals(1, buildDetails.size());
                    final BuildDetail buildDetail = buildDetails.get(0);
                    assertEquals(BuildDetailType.EXCEPTION_MESSAGE, buildDetail.getBuildDetailType());
                    assertTrue(new String(buildDetail.getDetail(), UTF_8).startsWith("Cannot run program \"" + invalidBuildCommand + "\""));
                }
            }

            // post-flight - Check queues that we consume are now empty
            message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

        } finally {
            instance.close();
        }
    }


    @Test
    public void fromBuildRequestToBenchmarkingFailed(@TempDir final Path tempDir) throws IOException, JMSException {
        assumeFalse(IS_WINDOWS);

        final String invalidBenchmarkCommand = "no-such-benchmark-command";
        final Runner.Settings settings = new Runner.Settings(Constants.DEFAULT_ARTEMIS_HOST, Constants.DEFAULT_ARTEMIS_PORT, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, tempDir, null, invalidBenchmarkCommand, true, true);
        final Runner runner = new Runner(settings);

        final JMSServiceInstance instance = runner.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            final String repository = "facebook/rocksdb";
            final String ref = "origin/refs/master";
            final String commit = "9dc887e";
            final String author = "adamretter";

            // send a BuildRequest to the BuildRequestQueue
            final BuildRequest buildRequest = new BuildRequest(repository, ref, commit, author);
            message = session.createTextMessage(buildRequest.serialize());
            producer.send(buildRequestQueue, message);

            /*
                Runner should receive and process the BuildRequest,
                and then send the following response messages in sequence:
                  - UPDATING_SOURCE
                  - UPDATING_SOURCE_COMPLETE
                  - BUILDING
                  - BUILDING_COMPLETE
                  - BENCHMARKING
                  - BENCHMARKING_FAILED
            */

            final Deque<BuildState> expectedBuildStates = Deque(
                    UPDATING_SOURCE,
                    UPDATING_SOURCE_COMPLETE,
                    BUILDING,
                    BUILDING_COMPLETE,
                    BENCHMARKING,
                    BENCHMARKING_FAILED
            );

            BuildState expectedBuildState = null;
            while ((expectedBuildState = expectedBuildStates.poll()) != null) {

                // check that the response is as expected
                message = buildResponseQueueConsumer.receive();
                assertNotNull(message);
                assertTrue(message instanceof TextMessage);

                final BuildResponse buildResponse = new BuildResponse().deserialize(((TextMessage)message).getText());

                assertEquals(expectedBuildState, buildResponse.getBuildState());
                assertEquals(buildRequest, buildResponse.getBuildRequest());

                if (expectedBuildState == BENCHMARKING_FAILED) {
                    final List<BuildDetail> buildDetails = buildResponse.getBuildDetails();
                    assertNotNull(buildDetails);
                    assertEquals(1, buildDetails.size());
                    final BuildDetail buildDetail = buildDetails.get(0);
                    assertEquals(BuildDetailType.EXCEPTION_MESSAGE, buildDetail.getBuildDetailType());
                    assertTrue(new String(buildDetail.getDetail(), UTF_8).startsWith("Cannot run program \"" + invalidBenchmarkCommand + "\""));
                }
            }

            // post-flight - Check queues that we consume are now empty
            message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

        } finally {
            instance.close();
        }
    }
}
