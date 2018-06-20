package com.nmote.mcf;

import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.MimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OutOfOfficeDeliveryAgent implements DeliveryAgent {

    @Inject
    public OutOfOfficeDeliveryAgent(Queue queue, MessageProcessor processor) {
        this.queue = Objects.requireNonNull(queue);
        this.processor = Objects.requireNonNull(processor);
    }

    public QueueMessage createOutOfOffice(QueueMessage message, Delivery delivery) throws IOException {
        QueueMessage reply = queue.newMessage();
        String returnPath = message.getHeader().get("Return-Path");
        if (returnPath == null) {
            returnPath = message.getFrom();
        }

        String subject = message.getHeader().get("Subject");
        if (subject == null) {
            subject = "Out of office";
        }

        String[] a = StringUtils.split(delivery.getDestination(), ":", 3);
        if (a.length != 3) {
            log.error("Invalid out-of-office format: {}", delivery.getDestination());
            return null;
        }

        String from = a[1];

        if (rateLimited(from, returnPath)) {
            log.info("Limited to max 1 message per hour {} => {}", from, returnPath);
            return null;
        }

        String text = a[2];
        reply.setFrom(from);

        StringBuilder b = new StringBuilder();
        b.append("Subject: Re: ").append(subject).append("\r\n");
        b.append("From: ").append(from).append("\r\n");
        b.append("To: ").append(returnPath).append("\r\n");
        b.append("Content-Type: text/plain;charset=utf-8\r\n");
        b.append("\r\n");
        b.append(text);
        b.append("\r\n");

        try {
            reply.create(new ByteArrayInputStream(b.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (MimeException e) {
            throw new IOException(e);
        }
        reply.setRecipients(Collections.singletonList(returnPath));
        return reply;
    }

    @Override
    public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
        delivery.setStatus("out-of-office");
        delivery.setCompleted();

        QueueMessage reply = createOutOfOffice(msg, delivery);
        if (reply != null) {
            log.info("Sending out of office reply");
            processor.route(reply);
            processor.deliver(reply);
            if (reply.isCompleted()) {
                reply.delete();
            }
        }
    }

    protected boolean rateLimited(String from, String to) {
        String key = from + ":" + to;
        long now = System.currentTimeMillis();
        synchronized (rateLimitter) {
            Long last = rateLimitter.get(key);
            if (last == null || (now - last) / 60_000 > 60) {
                rateLimitter.put(key, now);
                return false;
            }
        }
        return true;
    }

    private final Map<String, Long> rateLimitter = new HashMap<>();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Queue queue;
    private final MessageProcessor processor;
}
