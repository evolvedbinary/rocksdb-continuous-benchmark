package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.dataobject.WebHookPayloadSummary;

import java.io.IOException;

public class WebHookPayloadSummaryJmsProcessor implements WebHookPayloadSummaryProcessor {

    private final JMSClient jmsClient;

    public WebHookPayloadSummaryJmsProcessor(final JMSClient jmsClient) {
        this.jmsClient = jmsClient;
    }

    @Override
    public void process(final WebHookPayloadSummary webHookPayloadSummary) throws IOException {
        // Send the payload summary to a message queue
        jmsClient.sendMessage(webHookPayloadSummary);
    }
}
