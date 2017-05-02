package com.nmote.mcf.optima;

import com.nmote.counters.Counters;
import com.nmote.maildir.Maildir;
import com.nmote.mcf.DefaultMessageProcessor;
import com.nmote.mcf.DeliveryMessageProcessor;
import com.nmote.mcf.QueueMessage;
import com.nmote.mcf.RejectException;
import com.nmote.xr.XR;
import com.nmote.xr.XRMethod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OptimaMessageProcessor extends DefaultMessageProcessor {

    interface Omp {

        /**
         * Resolves email to list of delivery destinations
         *
         * @param email recepient address
         * @return list of delivery destinations
         */
        @XRMethod("omp.deliverEmail")
        ArrayList<String> deliverEmail(String email);
    }

    @Inject
    public OptimaMessageProcessor(
            @Named("ompAddress") String ompAddress,
            Counters counters,
            DeliveryMessageProcessor delivery
    ) throws URISyntaxException {
        this.omp = XR.proxy(new URI(ompAddress), Omp.class);
        this.counters = Objects.requireNonNull(counters);
        this.delivery = Objects.requireNonNull(delivery);
    }

    @Override
    public void check(QueueMessage message) throws IOException {
    }

    @Override
    public void checkRecipient(String recipient) throws RejectException {
        try {
            List<String> delivery = omp.deliverEmail(recipient);
            if (delivery.isEmpty()) {
                log.debug("Rejected {}", recipient);
                throw new RejectException();
            } else {
                log.debug("Accepted {} => {}", recipient, delivery);
            }
        } catch (Throwable t) {
            throw new RejectException();
        }
    }

    @Override
    public void deliver(QueueMessage message) throws IOException {
        delivery.deliver(message);
    }

    public void redeliver(QueueMessage message) throws IOException {
        delivery.redeliver(message);
    }

    @Override
    public void route(final QueueMessage message) throws IOException {
        // Local delivery to maildirs, bounces and remote delivery
        for (final String recipient : message.getRecipients()) {
            final long start = System.currentTimeMillis();
            MDC.put("to", recipient);
            try {
                // Resolve email address
                List<String> destinations = omp.deliverEmail(recipient);
                for (final String d : destinations) {
                    message.deliverTo(recipient, d);
                }
                log.debug("Routed to {}", destinations);
            } finally {
                MDC.remove("to");
                final long elapsed = System.currentTimeMillis() - start;
                counters.add("time.optima", elapsed);
                counters.add("count.optima", 1);
            }
        }
    }

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Omp omp;
    private Counters counters;
    private DeliveryMessageProcessor delivery;

}
