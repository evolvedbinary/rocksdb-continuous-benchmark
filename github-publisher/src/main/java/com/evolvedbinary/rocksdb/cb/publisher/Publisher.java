package com.evolvedbinary.rocksdb.cb.publisher;

import com.evolvedbinary.rocksdb.cb.dataobject.BuildResponse;
import com.evolvedbinary.rocksdb.cb.jms.AbstractJMSService;
import com.evolvedbinary.rocksdb.cb.jms.JMSServiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Publisher extends AbstractJMSService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);
    private static final AtomicReference<JMSServiceState> STATE = new AtomicReference<>(JMSServiceState.IDLE);

    private final Settings settings;
    private final OutputQueueMessageListener outputQueueMessageListener = new OutputQueueMessageListener();

    public Publisher(final Settings settings) {
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
        return "publisher";
    }

    @Override
    protected List<String> getQueueNames() {
        return Arrays.asList(
                settings.outputQueueName
        );
    }

    @Nullable
    @Override
    protected MessageListener getListener(final String queueName) {
        if (settings.outputQueueName.equals(queueName)) {
            return outputQueueMessageListener;
        }

        return null;
    }

    private class OutputQueueMessageListener implements MessageListener {
        @Override
        public void onMessage(final Message message) {
            if (!(message instanceof TextMessage)) {
                // acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected type {} from Queue: {}.", message.getClass().getName(), settings.outputQueueName);
                }

                // can't process non-text message, so DONE
                return;
            }

            final TextMessage textMessage = (TextMessage) message;
            final String content;
            try {
                content = textMessage.getText();
            } catch (final JMSException e) {
                LOGGER.error("Could not get content of TextMessage from Queue: {}. Error: {}", settings.outputQueueName, e.getMessage(), e);

                // can't access message content, so DONE
                return;
            }

            // TODO(AR) do we want this as a BuildResponse, or would something more "publishing" specific be better?

            // attempt to parse as BuildResponse
            final BuildResponse buildResponse;
            try {
                buildResponse = new BuildResponse().deserialize(content);
            } catch (final IOException e) {
                // unable to deserialize, acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected format from Queue: {}. Error: {}. Content: '{}'", settings.outputQueueName, e.getMessage(), content);
                }
                return;
            }

            // TODO(AR) implement publishing logic -- need to know what stats FB RocksDB Team would like

            // TODO(AR) don't forget to acknowledge the message once we have processed it!
        }
    }

    static class Settings {
        final String outputQueueName;
        final String repo;
        final String repoBranch;

        public Settings(final String outputQueueName, final String repo, final String repoBranch) {
            this.outputQueueName = outputQueueName;
            this.repo = repo;
            this.repoBranch = repoBranch;
        }
    }
}
