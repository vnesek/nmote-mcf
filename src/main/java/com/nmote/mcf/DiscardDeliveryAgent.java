package com.nmote.mcf;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nmote.counters.Counters;

public class DiscardDeliveryAgent implements DeliveryAgent {

	@Override
	public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
		String status = StringUtils.trimToNull(delivery.getDestination().substring(7));
		if (status == null) {
			status = "discarded";
		} else if (status.startsWith(":")) {
			status = status.substring(1);
		}
		delivery.setStatus(status);
		delivery.setCompleted();
		log.info("Discarded {}", msg.getId());
		counters.add("discard.count", 1);
	}

	private Logger log = LoggerFactory.getLogger(getClass());

	@Inject
	private Counters counters;
}
