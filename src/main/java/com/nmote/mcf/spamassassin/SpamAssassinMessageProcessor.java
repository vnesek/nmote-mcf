package com.nmote.mcf.spamassassin;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.nmote.mcf.DefaultMessageProcessor;
import com.nmote.mcf.QueueMessage;

public class SpamAssassinMessageProcessor extends DefaultMessageProcessor {

	private static final String HEADER_NAME = "X-MCF-Spam";

	@Override
	public void check(QueueMessage message) throws IOException {
		if (!message.getFlags().contains(QueueMessage.Flags.SPAM)) {
			long start = System.currentTimeMillis();
			SpamAssassinResult result = client.check(message.getInputStream(), message.getSize());
			long elapsed = System.currentTimeMillis() - start;
			if (result.isSpam()) {
				log.debug("Detected spam {} in {} ms => {}", message, elapsed, result);
				message.getHeader().add(HEADER_NAME, "True");
				message.getFlags().add(QueueMessage.Flags.SPAM);
			} else {
				log.debug("Checked {} in {} ms => {}", message, elapsed, result);
			}
			message.getHeader().add(HEADER_NAME + "Score", Float.toString(result.getScore()));
			message.getHeader().add(HEADER_NAME + "Threshold", Float.toString(result.getThreshold()));
			message.getHeader().add(HEADER_NAME + "Tests", result.getSymbols().toString());
		}
	}

	@Inject
	private SpamAssassinClient client;


	private Logger log = LoggerFactory.getLogger(getClass());
}
