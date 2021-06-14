package com.evolvedbinary.rocksdb.cb.runner;

import com.evolvedbinary.rocksdb.cb.dataobject.BuildRequest;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class RunnerIT {

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

    //TODO(AR) use a temp dir
    @Test
    public void buildRequest(/*@TempDir final Path tempDir */) throws IOException, JMSException {

        //TODO(AR) use a temp dir
        final Path tempDir = Files.createDirectories(Paths.get("/tmp/cb-test"));

        final Runner.Settings settings = new Runner.Settings(BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, tempDir, true);
        final Runner runner = new Runner(settings);

        final Runner.Instance instance = runner.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildResponseQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);

            // send a BuildRequest to the BuildRequestQueue
            final BuildRequest buildRequest = new BuildRequest("facebook/rocksdb", "origin/refs/master", "9dc887e", "adamretter");
            message = session.createTextMessage(buildRequest.serialize());
            producer.send(buildRequestQueue, message);

            /*
                Runner should receive and process the BuildRequest,
                and then create and send a BuildResponse.
            */

            // expect a BuildResponse on the BuildResponseQueue
            //message = buildRequestQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            message = buildResponseQueueConsumer.receive();
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);

            //TODO(AR) at the moment the above blocks as no BuildResponse is sent, we should have sent a build started one!

            //TODO(AR) implement these checks similar to OrchestratorIT




        } finally {
            instance.close();
        }
    }
}
