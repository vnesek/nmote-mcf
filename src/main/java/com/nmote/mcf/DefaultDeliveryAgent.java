package com.nmote.mcf;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        String protocol = StringUtils.substringBefore(dest, ":");
        switch (protocol) {
            case "smtp":
            case "remote":
                agent = smtp;
                break;

            case "mbox":
                agent = mbox;
                break;

            case "maildir":
                agent = maildir;
                break;

            case "discard":
            case "trash":
                agent = discard;
                break;

            case "bounce":
                agent = bounce;
                break;

            default:
                agent = null;
                break;
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

    @Inject(optional = true)
    protected BounceDeliveryAgent bounce;

    private Map<String, String> aliases;
}
