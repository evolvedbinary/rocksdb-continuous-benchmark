package com.evolvedbinary.rocksdb.cb.publisher;

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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class PublisherIT {

    private static final String PUBLISH_REQUEST_QUEUE_NAME = "TestPublishRequestQueue";
    private static final String PUBLISH_RESPONSE_QUEUE_NAME = "TestPublishResponseQueue";

    private static final int IMMEDIATE_TIMEOUT = -1;
    private static final int MESSAGE_RECEIVE_TIMEOUT = 1000;  // 1 second

    @RegisterExtension
    @Order(1)
    final JUnit5ExternalResourceAdapter<EmbeddedJMSResource> embeddedJmsExtension = new JUnit5ExternalResourceAdapter<>(new EmbeddedJMSResource(true));

    private Connection connection;
    private Session session;
    private Queue publishRequestQueue;
    private Queue publishResponseQueue;
    private MessageProducer producer;
    private MessageConsumer publishResponseQueueConsumer;

    @BeforeEach
    public void setup() throws JMSException {
        final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        this.connection = connectionFactory.createConnection();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.publishRequestQueue = session.createQueue(PUBLISH_REQUEST_QUEUE_NAME);
        this.publishResponseQueue = session.createQueue(PUBLISH_RESPONSE_QUEUE_NAME);

        this.producer = session.createProducer(null);

        this.publishResponseQueueConsumer = session.createConsumer(publishResponseQueue);

        connection.start();
    }

    @AfterEach
    public void tearDown() throws JMSException {
        if (connection != null) {
            connection.stop();
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
    public void fromBuildResponseToPublished(@TempDir final Path tempDir) throws IOException, JMSException {
        final Publisher.Settings settings = new Publisher.Settings(PUBLISH_REQUEST_QUEUE_NAME, PUBLISH_RESPONSE_QUEUE_NAME, tempDir, "adamretter/rocksdb-continuous-benchmark", "gh-pages", null, null, true);
        final Publisher publisher = new Publisher(settings);

        final JMSServiceInstance instance = publisher.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            try (final MessageConsumer outputQueueConsumer = session.createConsumer(publishRequestQueue)) {
                final Message message = outputQueueConsumer.receive(IMMEDIATE_TIMEOUT);
                assertNull(message);
            }
            try (final MessageConsumer outputQueueConsumer = session.createConsumer(publishResponseQueue)) {
                final Message message = outputQueueConsumer.receive(IMMEDIATE_TIMEOUT);
                assertNull(message);
            }

            // send a PublishRequest to the PublishRequestQueue
            final BuildRequest buildRequest = new BuildRequest("facebook/rocksdb", "master", "abcdef1", "adamretter");
            final BuildStats buildStats = new BuildStats(1234, 4321, 54321);
            final BuildResponse buildResponse = new BuildResponse(BuildState.BENCHMARKING_COMPLETE, buildRequest, buildStats, null);
            final PublishRequest publishRequest = new PublishRequest(buildResponse);
            Message message = session.createTextMessage(publishRequest.serialize());
            producer.send(publishRequestQueue, message);

            /*
                Publisher should receive and process the BuildResponse
            */

            // expect a PublishResponse on the PublishResponseQueue
            message = publishResponseQueueConsumer.receive();
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final PublishResponse publishResponse = new PublishResponse().deserialize(((TextMessage)message).getText());
            assertEquals(buildRequest, publishResponse.getBuildRequest());
            assertEquals(PublishState.COMPLETE, publishResponse.getPublishState());

        } finally {
            instance.close();
        }
    }
}
