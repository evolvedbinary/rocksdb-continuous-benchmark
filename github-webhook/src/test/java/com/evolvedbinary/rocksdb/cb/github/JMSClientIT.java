package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.Constants;
import com.evolvedbinary.rocksdb.cb.dataobject.WebHookPayloadSummary;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JMSClientIT {

    private static final String WEB_HOOK_QUEUE_NAME = "TestWebHookQueue";

    @RegisterExtension
    @Order(1)
    final JUnit5ExternalResourceAdapter<EmbeddedJMSResource> embeddedJmsExtension = new JUnit5ExternalResourceAdapter<>(new EmbeddedJMSResource(true));

    private Connection connection;
    private Session session;
    private Queue webHookQueue;
    private MessageConsumer webHookQueueConsumer;

    @BeforeEach
    public void setup() throws JMSException {
        final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        this.connection = connectionFactory.createConnection();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.webHookQueue = session.createQueue(WEB_HOOK_QUEUE_NAME);

        this.webHookQueueConsumer = session.createConsumer(webHookQueue);

        connection.start();
    }

    @AfterEach
    public void tearDown() throws JMSException {
        if (connection != null) {
            connection.stop();
        }

        if (webHookQueueConsumer != null) {
            webHookQueueConsumer.close();
        }

        if (session != null) {
            session.close();
        }

        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void sendMessage() throws JMSException, IOException {
        final EmbeddedJMSResource embeddedJmsResource = embeddedJmsExtension.getExternalResource();

        assertEquals(0, embeddedJmsResource.getMessageCount(WEB_HOOK_QUEUE_NAME));

        final JMSClient.Settings settings = new JMSClient.Settings(Constants.DEFAULT_ARTEMIS_HOST, Constants.DEFAULT_ARTEMIS_PORT, WEB_HOOK_QUEUE_NAME);
        final JMSClient client = new JMSClient(settings);
        try {
            client.start();

            final WebHookPayloadSummary dataObject = new WebHookPayloadSummary("origin/refs/master", "abc", "def", "facebook/rocksdb", "person1", "person2");
            client.sendMessage(dataObject);

            assertEquals(1, embeddedJmsResource.getMessageCount(WEB_HOOK_QUEUE_NAME));

            final Message message = webHookQueueConsumer.receive(2500);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final TextMessage textMessage = (TextMessage) message;

            final byte[] buf = textMessage.getText().getBytes(UTF_8);
            final WebHookPayloadSummary actualDataObject;
            try (final ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
                actualDataObject = new WebHookPayloadSummary().deserialize(bais);
            }
            assertEquals(dataObject, actualDataObject);

        } finally {
            client.stop();
        }
    }
}
