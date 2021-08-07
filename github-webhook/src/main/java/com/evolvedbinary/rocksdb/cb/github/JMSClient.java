package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.dataobject.DataObject;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.util.Map;

import static com.evolvedbinary.rocksdb.cb.common.CloseUtil.closeAndLogIfException;
import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Entry;
import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Map;

public class JMSClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSLHandlerProvider.class);

    private final Settings settings;
    private Connection connection;
    private Session session;
    private Queue webHookQueue;
    private MessageProducer producer;
    private boolean started;

    public JMSClient(final Settings settings) {
        this.settings = settings;
    }

    public void start() throws IOException {
        try {
            final Map<String, Object> transportParams = Map(
                    Entry("host", settings.artemisBrokerHost),
                    Entry("port", settings.artemisBrokerPort)
            );
            final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName(), transportParams);
            final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

            this.connection = connectionFactory.createConnection();
            this.connection.setClientID("github-webhook");

            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            this.webHookQueue = session.createQueue(settings.webHookQueueName);
            this.producer = session.createProducer(null);

            this.started = true;
        } catch (final JMSException e) {
            throw new IOException("Unable to start JMSClient: " + e.getMessage(), e);
        }
    }

    public void stop() {
        this.started = false;
        closeAndLogIfException(producer, LOGGER);
        closeAndLogIfException(session, LOGGER);
        closeAndLogIfException(connection, LOGGER);
    }

    public void sendMessage(final DataObject message) throws IOException {
        if (!started) {
            throw new IOException("JMSClient is not started, you must call JMSClient#start() first!");
        }

        try {
            connection.start();
            try {
                final String text = message.serialize();
                final TextMessage textMessage = session.createTextMessage(text);
                producer.send(webHookQueue, textMessage);

                LOGGER.info("Sent {} to Queue: {}", message.getClass().getName(), settings.webHookQueueName);

            } finally {
                connection.stop();
            }
        } catch (final JMSException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        stop();
    }

    static class Settings {
        final String artemisBrokerHost;
        final int artemisBrokerPort;
        final String webHookQueueName;

        Settings(final String artemisBrokerHost, final int artemisBrokerPort, final String webHookQueueName) {
            this.artemisBrokerHost = artemisBrokerHost;
            this.artemisBrokerPort = artemisBrokerPort;
            this.webHookQueueName = webHookQueueName;
        }
    }
}
