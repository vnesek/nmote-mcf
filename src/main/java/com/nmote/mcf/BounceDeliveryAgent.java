package com.nmote.mcf;

import org.apache.james.mime4j.MimeException;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

public class BounceDeliveryAgent implements DeliveryAgent {

    @Inject
    public BounceDeliveryAgent(Queue queue, MessageProcessor processor) {
        this.queue = Objects.requireNonNull(queue);
        this.processor = Objects.requireNonNull(processor);
    }

    public QueueMessage createBounce(QueueMessage message) throws IOException {
        QueueMessage bounce = queue.newMessage();
        String returnPath = message.getHeader().get("Return-Path");
        if (returnPath == null) {
            returnPath = message.getFrom();
        }

        StringBuilder b = new StringBuilder();
        b.append("Subject: Failure notice\r\n");
        b.append("From: mailer-daemon\r\n");
        b.append("To: ").append(returnPath).append("\r\n");
        b.append("\r\n");
        b.append("Bounced\n\n--- Below this line is a copy of the message.\n\n");

        InputStream in = new SequenceInputStream(
                new ByteArrayInputStream(b.toString().getBytes(StandardCharsets.US_ASCII)),
                message.getInputStream()
        );
        try {
            bounce.create(in);
        } catch (MimeException e) {
            throw new IOException(e);
        }
        bounce.setRecipients(Collections.singletonList(returnPath));
        return bounce;
    }

    @Override
    public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
        delivery.setStatus("bounced");
        delivery.setCompleted();

        QueueMessage b = createBounce(msg);
        processor.route(b);
        processor.deliver(b);
        if (b.isCompleted()) {
            b.delete();
        }
    }

    private final Queue queue;
    private final MessageProcessor processor;
}
