package com.nmote.mcf;

import java.io.IOException;

/**
 * Handles message processing.
 */
public interface MessageProcessor {

    /**
     * Checks message recipient. Called before actual QueueMessage is available.
     *
     * @param recipient
     * @throws IOException
     */
    void checkRecipient(String recipient) throws RejectException;

    /**
     * Performs antivirus/antispam checks on a message prior to routing.
     *
     * @param message
     * @throws IOException
     */
    void check(QueueMessage message) throws IOException;

    /**
     * Called after message is received from network to route it to it's
     * destinations.
     *
     * @param message
     * @throws IOException
     */
    void route(QueueMessage message) throws IOException;

    /**
     * Called after message is routed to deliver message to it's recipients.
     *
     * @param message
     * @throws IOException
     */
    void deliver(QueueMessage message) throws IOException;

    /**
     * Called to redeliver message whose delivery was temporarily impossible.
     *
     * @param message
     * @throws IOException
     */
    void redeliver(QueueMessage message) throws IOException;
}
