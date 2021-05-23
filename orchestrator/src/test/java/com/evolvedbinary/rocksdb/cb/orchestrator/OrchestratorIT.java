package com.evolvedbinary.rocksdb.cb.orchestrator;

import com.evolvedbinary.rocksdb.cb.dataobject.BuildRequest;
import com.evolvedbinary.rocksdb.cb.dataobject.BuildResponse;
import com.evolvedbinary.rocksdb.cb.dataobject.BuildState;
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
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class OrchestratorIT {

    private static final String WEB_HOOK_QUEUE_NAME = "TestWebHookQueue";
    private static final String BUILD_REQUEST_QUEUE_NAME = "TestBuildRequestQueue";
    private static final String BUILD_RESPONSE_QUEUE_NAME = "TestBuildResponseQueue";
    private static final String OUTPUT_QUEUE_NAME = "TestOutputQueue";

    private static final int IMMEDIATE_TIMEOUT = -1;
    private static final int MESSAGE_RECEIVE_TIMEOUT = 1000;  // 1 second

    @RegisterExtension @Order(1)
    final JUnit5ExternalResourceAdapter<EmbeddedJMSResource> embeddedJmsExtension = new JUnit5ExternalResourceAdapter<>(new EmbeddedJMSResource(true));

    private Connection connection;
    private Session session;
    private Queue webHookQueue;
    private Queue buildRequestQueue;
    private Queue buildResponseQueue;
    private Queue outputQueue;
    private MessageProducer producer;
    private MessageConsumer buildRequestQueueConsumer;
    private MessageConsumer outputQueueConsumer;

    @BeforeEach
    public void setup() throws JMSException {
        final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        this.connection = connectionFactory.createConnection();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.webHookQueue = session.createQueue(WEB_HOOK_QUEUE_NAME);
        this.buildRequestQueue = session.createQueue(BUILD_REQUEST_QUEUE_NAME);
        this.buildResponseQueue = session.createQueue(BUILD_RESPONSE_QUEUE_NAME);
        this.outputQueue = session.createQueue(OUTPUT_QUEUE_NAME);

        this.producer = session.createProducer(null);
        this.buildRequestQueueConsumer = session.createConsumer(buildRequestQueue);
        this.outputQueueConsumer = session.createConsumer(outputQueue);

        connection.start();
    }

    @AfterEach
    public void tearDown() throws JMSException {
        if (connection != null) {
            connection.stop();
        }

        if (outputQueueConsumer != null) {
            outputQueueConsumer.close();
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
    public void fromHookToOutputNoRefFiltersAllBuilds() throws IOException, JMSException {
        final Orchestrator.Settings settings = new Orchestrator.Settings(WEB_HOOK_QUEUE_NAME, BUILD_REQUEST_QUEUE_NAME, BUILD_RESPONSE_QUEUE_NAME, OUTPUT_QUEUE_NAME, Collections.emptyList(), true);
        final Orchestrator orchestrator = new Orchestrator(settings);

        final Orchestrator.Instance instance = orchestrator.runAsync();
        try {

            // pre-flight - Check queues that we will consume are empty
            Message message = buildRequestQueueConsumer.receive(IMMEDIATE_TIMEOUT);
            assertNull(message);
            message = outputQueueConsumer.receive(IMMEDIATE_TIMEOUT);
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

            // Send a BuildResponse(BUILDING) to the BuildResponseQueue
            final BuildResponse buildResponseBuilding = new BuildResponse(BuildState.BUILDING, buildRequest);
            message = session.createTextMessage(buildResponseBuilding.serialize());
            producer.send(buildResponseQueue, message);

            /*
                Orchestrator should received and process the BuildResponse(BUILDING),
                and update its internal state for the build.
            */

            // Check output queue is empty
            message = outputQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNull(message);

            // Send a BuildResponse(BUILT) to the BuildResponseQueue
            final BuildResponse buildResponseBuilt = new BuildResponse(BuildState.BUILT, buildRequest);
            message = session.createTextMessage(buildResponseBuilt.serialize());
            producer.send(buildResponseQueue, message);

            /*
                Orchestrator should received and process the BuildResponse(BUILT),
                update its internal state for the build, and then send the output
                to the OutputQueue
            */

            // expect a BuildResponse on the OutputQueue
            message = outputQueueConsumer.receive(MESSAGE_RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertTrue(message instanceof TextMessage);
            final BuildResponse outputBuildResponse = new BuildResponse().deserialize(((TextMessage)message).getText());
            assertEquals(buildResponseBuilt, outputBuildResponse);

        } finally {
            instance.close();
        }
    }
}
