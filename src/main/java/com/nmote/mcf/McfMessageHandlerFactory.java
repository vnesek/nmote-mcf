package com.nmote.mcf;

import com.google.inject.Inject;
import com.nmote.counters.Counters;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;

public class McfMessageHandlerFactory implements MessageHandlerFactory {

    @Inject
    public McfMessageHandlerFactory(Queue queue, MessageProcessor processor, Counters counters) {
        setQueue(queue);
        setProcessor(processor);
        this.counters = counters;
    }

    public MessageHandler create(MessageContext ctx) {
        return new McfMessageHandler(queue, processor, ctx, counters);
    }

    public void setProcessor(MessageProcessor processor) {
        if (processor == null) {
            throw new NullPointerException("processor == null");
        }
        this.processor = processor;
    }

    public void setQueue(Queue queue) {
        if (queue == null) {
            throw new NullPointerException("queue == null");
        }
        this.queue = queue;
    }

    private Counters counters;
    private Queue queue;
    private MessageProcessor processor;
}
