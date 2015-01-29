package com.nmote.mcf.clamav;

import java.io.IOException;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.nmote.mcf.DefaultMessageProcessor;
import com.nmote.mcf.QueueMessage;

public class ClamAVMessageProcessor extends DefaultMessageProcessor {

	@Override
	public void check(QueueMessage message) throws IOException {
		if (!message.getFlags().contains(QueueMessage.Flags.VIRUS)) {
			long start = System.currentTimeMillis();

			ClamAVClient cavc = client.get();
			try {
				String result = cavc.instream(message.getInputStream());
				long elapsed = System.currentTimeMillis() - start;
				if (result != null) {
					log.debug("Detected virus {} in {} ms", result, elapsed);
					message.getHeader().add("X-MCF-ClamAV", result);
					message.getFlags().add(QueueMessage.Flags.VIRUS);
				} else {
					log.debug("Checked {} in {} ms", message, elapsed);
				}
			} finally {
				cavc.close();
			}
		}
	}

	@Inject
	private Provider<ClamAVClient> client;

	private Logger log = LoggerFactory.getLogger(getClass());
}
