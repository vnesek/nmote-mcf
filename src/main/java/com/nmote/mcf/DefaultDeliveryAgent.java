package com.nmote.mcf;

import java.io.IOException;

import javax.inject.Inject;

public class DefaultDeliveryAgent implements DeliveryAgent {

	@Override
	public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
		DeliveryAgent agent;
		String dest = delivery.getDestination();
		if (dest.startsWith("smtp:")) {
			agent = smtp;
		} else if (dest.startsWith("mbox:")) {
			agent = mbox;
		} else if (dest.startsWith("maildir:")) {
			agent = maildir;
		} else if (dest.startsWith("discard")) {
			agent = discard;
		} else {
			throw new IOException("unknown delivery protocol: " + dest);
		}
		agent.deliver(msg, delivery);
	}

	@Inject
	private SmtpDeliveryAgent smtp;
	
	@Inject
	private MBoxDeliveryAgent mbox;
	
	@Inject
	private MaildirDeliveryAgent maildir;
	
	@Inject
	private DiscardDeliveryAgent discard;
}
