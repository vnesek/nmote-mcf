package com.nmote.mcf;

import java.io.IOException;

public class DefaultMessageProcessor implements MessageProcessor {

    @Override
    public void check(QueueMessage message) throws IOException {
    }

    @Override
    public void checkRecipient(String recipient) throws RejectException {
    }

    @Override
    public void deliver(QueueMessage message) throws IOException {
    }

    @Override
    public void redeliver(QueueMessage message) throws IOException {
    }

    @Override
    public void route(QueueMessage message) throws IOException {
    }
}
