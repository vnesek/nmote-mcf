package com.nmote.mcf;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Delivers messages to it's destinations. Marks messages as spam and skips
 * delivery of virus infected messages.
 */
public class DeliveryMessageProcessor extends DefaultMessageProcessor {

    @Override
    public void deliver(final QueueMessage message) throws IOException {
        Set<String> flags = message.getFlags();

        // Check if message is spam
        if (flags.contains(QueueMessage.Flags.SPAM)) {
            if (keepSpam) {
                if (spamPrefix != null) {
                    String subject = message.getHeader().get("subject");
                    if (subject == null) {
                        subject = "(No subject)";
                    } else {
                        subject = DecoderUtil.decodeEncodedWords(subject, DecodeMonitor.SILENT, StandardCharsets.UTF_8);
                    }
                    subject = spamPrefix + subject;
                    subject = EncoderUtil.encodeIfNecessary(subject, EncoderUtil.Usage.TEXT_TOKEN, 0);
                    message.getHeader().set("subject", subject);
                    log.info("Marked spam message");
                } else {
                    log.info("Delivering spam message");
                }
            } else {
                message.setCompleted("Spam detected");
                log.info("Ignored spam message");
                return;
            }
        }

        // Check if message contains a virus
        if (flags.contains(QueueMessage.Flags.VIRUS)) {
            message.setCompleted("Virus detected");
            log.info("Ignored virus infected message");
            return;
        }

        // Ok, now let's deliver it
        redeliver(message);
    }

    @Override
    public void redeliver(QueueMessage message) throws IOException {
        // Deliver it. We are using index and size() to allow delivery agents to
        // append additional deliveries.
        List<Delivery> deliveries = message.getDeliveries();
        for (Delivery d : deliveries) {
            if (!d.isCompleted()) {
                if (log.isDebugEnabled()) {
                    log.debug("Delivering {}", d);
                }
                try {
                    deliveryAgent.deliver(message, d);
                } catch (IOException e) {
                    d.setStatus("Failed " + e.getMessage());
                    log.error("Delivery IO problem {}", e.getMessage());
                }
            }
        }
    }

    @Inject
    @Named("keepSpam")
    private boolean keepSpam;

    @Inject
    @Named("spamPrefix")
    private String spamPrefix;

    @Inject
    private DeliveryAgent deliveryAgent;

    private Logger log = LoggerFactory.getLogger(getClass());
}
