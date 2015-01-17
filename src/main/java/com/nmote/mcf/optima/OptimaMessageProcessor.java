package com.nmote.mcf.optima;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nmote.maildir.Maildir;
import com.nmote.mcf.DefaultMessageProcessor;
import com.nmote.mcf.NonLocalUserException;
import com.nmote.mcf.QueueMessage;
import com.nmote.mcf.RejectException;
import com.nmote.mcf.clamav.ClamAVMessageProcessor;
import com.nmote.mcf.spamassassin.SpamAssassinMessageProcessor;
import com.nmote.xr.Fault;
import com.nmote.xr.XR;

public class OptimaMessageProcessor extends DefaultMessageProcessor {

	@Inject
	public OptimaMessageProcessor(@Named("ompAddress") String ompAddress) throws URISyntaxException {
		routing = XR.proxy(new URI(ompAddress), Routing.class);
	}

	@Override
	public void check(QueueMessage message) throws IOException {
		// SpamAssassin
		try {
			spamAssassin.check(message);
		} catch (IOException ioe) {
			log.error("SpamAssassin check failed for {}", message, ioe);
		}

		// ClamAV
		try {
			clamAV.check(message);
		} catch (IOException ioe) {
			log.error("ClamAV check failed for {}", message, ioe);
		}
	}

	@Override
	public void checkRecipient(String recipient) throws RejectException {
		try {
			String address = routing.resolveEmail(recipient, "optinet.hr");
			if (address.equalsIgnoreCase(recipient)) {
				log.debug("Accepted {}", recipient);
			} else {
				log.debug("Accepted {} => {}", recipient, address);
			}
		} catch (Fault f) {
			throw new NonLocalUserException();
		} catch (Throwable t) {
			throw new RejectException();
		}
	}

	@Override
	public void route(QueueMessage message) throws IOException {
		// Only local delivery to existing vpopmail accounts
		for (String recipient : message.getRecipients()) {
			// Resolve email address
			String address = routing.resolveEmail(recipient, "optinet.hr");
			String[] a = StringUtils.split(address, '@');
			if (a.length != 2) {
				log.error("Invalid recipient {} for {}", recipient, message);
				throw new IOException("invalid address: " + recipient);
			}

			// Find a maildir
			Maildir maildir = maildirSource.get(a[0], a[1]);

			// TODO Parse .qmail file for forwards (should it be handled by maildir delivery agent?)
			message.deliverTo(recipient, maildir.toString());
			log.info("Routed to {}", maildir);
		}
	}

	@Inject
	private ClamAVMessageProcessor clamAV;

	private Logger log = LoggerFactory.getLogger(getClass());

	@Inject
	private MaildirSource maildirSource;

	private Routing routing;

	@Inject
	private SpamAssassinMessageProcessor spamAssassin;
}
