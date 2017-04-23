package com.nmote.mcf;

import java.io.IOException;

public interface DeliveryAgent {

    void deliver(QueueMessage msg, Delivery delivery) throws IOException;
}
