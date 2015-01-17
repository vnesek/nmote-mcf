package com.nmote.mcf;

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.MimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.server.Session;

import com.nmote.counters.Counters;

public class McfMessageHandler implements MessageHandler {

	public McfMessageHandler(Queue queue, MessageProcessor processor, MessageContext ctx, Counters counters) {
		this.ctx = ctx;
		this.queue = queue;
		this.processor = processor;
		this.counters = counters;
		this.counters.add("count.input", 1);
		this.start = System.currentTimeMillis();
	}

	public void data(InputStream data) throws RejectException, TooMuchDataException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Data");
		}

		try {
			// Build QueueMessage from input data and envelope
			this.msg.create(data);

			// First chech message for viruses
			processor.check(this.msg);

			// Now route it
			processor.route(this.msg);
			if (log.isDebugEnabled()) {
				log.debug("Message routed {}", msg.getDeliveries());
			}

			// ... and then deliver it
			if (!msg.isCompleted()) {
				processor.deliver(this.msg);
			} else {
				log.info("Skipping delivery");
			}

			// Format data command response. Append all available statuses.
			if (ctx instanceof Session) {
				Session session = (Session) ctx;
				session.setDataResponse("id=" + this.msg.getId());
				for (Delivery d : this.msg.getDeliveries()) {
					if (d.isCompleted()) {
						String status = d.getStatus();
						session.appendDataResponse(status);

						// Check for pass-reject-XXX status and reject in that case
						if (status.startsWith("pass-reject-")) {
							int code;
							try {
								// Remove message
								this.msg.setCompleted();
								this.msg.delete();

								// Extract SMTP reject code
								log.debug("Message rejected: {}", status);
								code = Integer.parseInt(status.substring(12, status.indexOf(' ', 13)));
							} catch (Throwable t) {
								log.error("Internal error while handling pass-reject status code", t);
								throw new RejectException("message rejected, internal error");
							}

							// Substitute 4xx for 5xx
							if (code / 100 == 4) {
								code += 100;
							}
							throw new RejectException(code, status);
						}
					}
				}
			}

			if (this.msg.isCompleted()) {
				counters.add("count.completed", 1);
				try {
					this.msg.delete();
					log.debug("Message completed");
				} catch (Exception e) {
					log.error("Failed to delete a message", e);
					// This is really weird... no use in informing client though
				}
			} else {
				counters.add("count.queued", 1);
				try {
					this.msg.close();
					log.debug("Message queued for redelivery");
				} catch (Exception e) {
					log.error("Failed to queue a message", e);
					throw new RejectException("message not queued");
				}
			}
		} catch (IOException ioe) {
			counters.add("count.io-error", 1);
			this.msg.delete();
			log.debug("IO problem while processing message, deleted: " + ioe);
			throw ioe;
		} catch (MimeException me) {
			counters.add("count.mime-error", 1);
			this.msg.delete();
			log.debug("MIME invalid message, deleted: " + me);
			throw new RejectException("invalid message: " + me);
		}
	}

	public void done() {
		MDC.remove("message");
		MDC.remove("remote");

		long elapsed = System.currentTimeMillis() - start;
		counters.add("time.processing", elapsed);
		counters.add("count.processing", 1);
		//log.debug("Counters {}", counters);

		this.ctx = null;
		this.msg = null;
		this.processor = null;
	}

	public void from(String from) throws RejectException {
		try {
			this.msg = queue.newMessage();

			MDC.put("message", msg.getId());
			MDC.put("remote", ctx.getRemoteAddress().toString());

			if (log.isDebugEnabled()) {
				log.debug("From: {}", from);
			}
			this.msg.setFrom(from);
		} catch (IOException e) {
			throw new RejectException("failed to create message");
		}
	}

	public void recipient(String recipient) throws RejectException {
		try {
			processor.checkRecipient(recipient);
			if (log.isDebugEnabled()) {
				log.debug("Rcpt {}", recipient);
			}
			this.msg.getRecipients().add(recipient);
		} catch (OverQuotaException e) {
			log.error("Rcpt over quota {}", recipient);
			throw new RejectException(452, "recipent over quota");
		} catch (NonLocalUserException e) {
			log.error("Rcpt not local {}", recipient);
			throw new RejectException(551, "user not local");
		} catch (TooManyForwardsException e) {
			log.error("Rcpt has too many local forwards {}", recipient);
			throw new RejectException(553, "too many forwards");
		} catch (Exception e) {
			log.error("Problem with recipient " + recipient, e);
			throw new RejectException(552, "transaction failed: " + e.getMessage());
		}
	}

	private Queue queue;
	private MessageContext ctx;
	private QueueMessage msg;
	private MessageProcessor processor;
	private Counters counters;
	private long start;

	private static final Logger log = LoggerFactory.getLogger(McfMessageHandler.class);
}
