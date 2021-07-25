package com.evolvedbinary.rocksdb.cb.jms;

import com.evolvedbinary.rocksdb.cb.dataobject.DataObject;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.jms.*;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static com.evolvedbinary.rocksdb.cb.common.CloseUtil.closeAndLogIfException;

public abstract class AbstractJMSService implements JMSService {

    private Connection connection;
    private Session session;
    private Map<String, Queue> queues;
    private @Nullable MessageProducer producer;
    private TreeMap<String, MessageConsumer> queueConsumers;

    protected abstract Logger getLogger();

    protected abstract AtomicReference<JMSServiceState> getState();

    protected abstract String getClientId();

    @Override
    public void runSync() throws InterruptedException {
        final JMSServiceInstance instance = runAsync();
        instance.awaitShutdown();
    }

    @Override
    public JMSServiceInstance runAsync() {
        if (!getState().compareAndSet(JMSServiceState.IDLE, JMSServiceState.RUNNING)) {
            throw new IllegalStateException("Already running");
        }

        // setup JMS
        final TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        final ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        try {
            final String clientId = getClientId();
            this.connection = createConnection(connectionFactory, clientId);
            this.session = createSession(connection);

            final List<String> queueNames = getQueueNames();
            if (queueNames != null) {
                for (final String queueName : queueNames) {
                    final Queue queue = createQueue(session, queueName);
                    if (queues == null) {
                        queues = new HashMap<>();
                    }
                    queues.put(queueName, queue);

                    final MessageListener listener = getListener(queueName);
                    if (listener != null) {
                        final MessageConsumer consumer = session.createConsumer(queue);
                        consumer.setMessageListener(listener);
                        if (queueConsumers == null) {
                            queueConsumers = new TreeMap<>();
                        }
                        queueConsumers.put(queueName, consumer);
                        getLogger().info("Listening to Queue: {}", queueName);
                    }
                }
            }

            this.producer = createProducer(session);

            // start the connection
            this.connection.start();

            getLogger().info("Created connection for ClientID: {}", clientId);

            final ExecutorService executorService = Executors.newFixedThreadPool(1, r -> new Thread(r, clientId + "-JMSService-Thread"));
            final Future<?> jmsServiceFuture = executorService.submit(new JMSServiceCallable());

            return new JMSServiceInstance(executorService, clientId, getState(), jmsServiceFuture);

        } catch (final JMSException e) {
            if (this.connection != null) {
                closeAndLogIfException(connection::stop, this::getLogger);
            }

            for (final MessageConsumer queueConsumer : queueConsumers.descendingMap().values()) {
                closeAndLogIfException(queueConsumer, this::getLogger);
            }

            if (this.producer != null) {
                closeAndLogIfException(this.producer, this::getLogger);
            }

            closeAndLogIfException(this.session, this::getLogger);
            closeAndLogIfException(this.connection, this::getLogger);

            throw new RuntimeException("Unable to setup JMS broker connection: " + e.getMessage(), e);
        }
    }

    protected Connection createConnection(final ConnectionFactory connectionFactory, final String clientId) throws JMSException {
        final Connection connection = connectionFactory.createConnection();
        connection.setClientID(clientId);
        return connection;
    }

    protected Session createSession(final Connection connection) throws JMSException {
        return connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    }

    protected abstract @Nullable List<String> getQueueNames();

    protected Queue createQueue(final Session session, final String queueName) throws JMSException {
        return session.createQueue(queueName);
    }

    protected abstract @Nullable MessageListener getListener(final String queueName);

    protected MessageProducer createProducer(final Session session) throws JMSException {
        return session.createProducer(null);
    }

    protected @Nullable Queue getQueue(final String queueName) {
        return queues.get(queueName);
    }

    protected boolean acknowledgeMessage(final Message message) {
        try {
            message.acknowledge();
            return true;
        } catch (final JMSException e) {
            getLogger().error("Unable to acknowledge message: {}", e.getMessage(), e);
            return false;
        }
    }

    protected void sendMessage(final DataObject message, final Queue queue) throws IOException, JMSException {
        // send the message
        final String content = message.serialize();
        final TextMessage textMessage = session.createTextMessage(content);
        producer.send(queue, textMessage);
        getLogger().info("Sent {} to Queue: {}", message.getClass().getName(), queue.getQueueName());
    }

    protected class JMSServiceCallable implements Callable<Void> {

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
                    closeAndLogIfException(connection::stop, AbstractJMSService.this::getLogger);
                }

                for (final MessageConsumer queueConsumer : queueConsumers.descendingMap().values()) {
                    closeAndLogIfException(queueConsumer, AbstractJMSService.this::getLogger);
                }

                if (producer != null) {
                    closeAndLogIfException(producer, AbstractJMSService.this::getLogger);
                }

                closeAndLogIfException(session, AbstractJMSService.this::getLogger);
                closeAndLogIfException(connection, AbstractJMSService.this::getLogger);

                throw e;
            }
        }
    }
}
