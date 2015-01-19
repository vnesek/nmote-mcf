package com.nmote.mcf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;

public class DefaultDeliveryAgent implements DeliveryAgent {

	@Override
	public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
		String dest = delivery.getDestination();
		if (aliases != null) {
			String r = aliases.get(dest);
			if (r != null) {
				dest = r;
			}
		}
		final DeliveryAgent agent = deliveryAgentFor(dest);
		if (agent == null) {
			throw new IOException("unsupported delivery protocol: " + delivery.getDestination());
		}
		agent.deliver(msg, delivery);
	}

	@Inject
	public void setFileIntoAlias(@Named("fileIntoAlias") String alias) {
		Map<String, String> aliases = new HashMap<>();
		for (String a : StringUtils.split(alias)) {
			String[] b = StringUtils.split(a, "=>");
			if (b.length != 2) {
				throw new IllegalArgumentException("invalid fileIntoAlias: " + a);
			}
			aliases.put(b[0], b[1]);
		}
		if (!aliases.isEmpty()) {
			this.aliases = aliases;
		}
	}

	protected DeliveryAgent deliveryAgentFor(String dest) {
		DeliveryAgent agent;
		if (dest.startsWith("smtp:")) {
			agent = smtp;
		} else if (dest.startsWith("mbox:")) {
			agent = mbox;
		} else if (dest.startsWith("maildir:")) {
			agent = maildir;
		} else if (dest.startsWith("discard")) {
			agent = discard;
		} else {
			agent = null;
		}
		return agent;
	}

	@Inject(optional = true)
	protected DiscardDeliveryAgent discard;

	@Inject(optional = true)
	protected MaildirDeliveryAgent maildir;

	@Inject(optional = true)
	protected MBoxDeliveryAgent mbox;

	@Inject(optional = true)
	protected SmtpDeliveryAgent smtp;

	private Map<String, String> aliases;
}
