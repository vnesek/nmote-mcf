package com.nmote.mcf.optima;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nmote.counters.Counters;
import com.nmote.maildir.Maildir;
import com.nmote.maildir.Quota;
import com.nmote.mcf.*;
import com.nmote.xr.EndpointBuilder;
import com.nmote.xr.XRMethod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class OptimaMessageProcessor extends DefaultMessageProcessor {

    @Inject
    public OptimaMessageProcessor(
            @Named("ompAddress") String ompAddress,
            Counters counters,
            DeliveryMessageProcessor delivery,
            @Named("pathRename") String pathRename,
            @Named("ompResolveCache") String resolveCache
    ) throws URISyntaxException {
        this.omp = new CachedOmp(EndpointBuilder.client(ompAddress, Omp.class).get(), resolveCache);
        this.counters = Objects.requireNonNull(counters);
        this.delivery = Objects.requireNonNull(delivery);
        this.pathRename = new PathRename(pathRename);
    }

    @Override
    public void check(QueueMessage message) throws IOException {
    }

    @Override
    public void checkRecipient(String recipient) throws RejectException {
        List<String> delivery;
        try {
            delivery = omp.deliverEmail(recipient);
        } catch (Throwable t) {
            log.error("Failed to check recipient", t);
            throw new RejectException();
        }
        if (delivery.isEmpty()) {
            log.debug("Rejected {}", recipient);
            throw new RejectException();
        } else {
            // Check quota for maildirs
            for (String d : delivery) {
                if (d.startsWith("maildir:")) {
                    Maildir m = new Maildir(new File(pathRename.rename(d.substring(8))));
                    if (!m.maildirSizeExists()) {
                        Quota quota = m.getQuota(Integer.MAX_VALUE, Long.MAX_VALUE);
                        if (quota.isOverQuota()) {
                            log.info("Over quota {} => {}, {}", recipient, m, quota);
                            throw new OverQuotaException();
                        }
                    }
                }
            }
            log.debug("Accepted {} => {}", recipient, delivery);
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
                    switch (StringUtils.substringBefore(d, ":")) {
                        case "remote":
                            message.deliverTo(StringUtils.substringAfter(d, ":"), d);
                            // message.deliverTo(recipient, "bounce:test");
                            break;
                        case "maildir":
                            message.deliverTo(recipient, pathRename.rename(d));
                            break;
                        case "out-of-office":
                        case "bounce":
                            message.deliverTo(recipient, d);
                            break;
                        case "pipe":
                        default:
                            log.info("Unsupported {}", d);
                    }
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

    interface Omp {

        /**
         * Resolves email to list of delivery destinations
         *
         * @param email recepient address
         * @return list of delivery destinations
         */
        @XRMethod("omp.deliverEmail")
        ArrayList<String> deliverEmail(String email);

        @XRMethod("omp.getEffectiveSettings")
        HashMap<String, String> getEffectiveSettings(String email);
    }

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Omp omp;
    private final PathRename pathRename;
    private Counters counters;
    private DeliveryMessageProcessor delivery;

    private static class CachedOmp extends CacheLoader<String, ArrayList<String>> implements Omp {

        CachedOmp(Omp delegate, String cacheSpec) {
            this.cache = CacheBuilder.from(cacheSpec).build(this);
            this.delegate = Objects.requireNonNull(delegate);
            this.bounce = new ArrayList<>();
            this.bounce.add("bounce:route-failed");
        }

        @Override
        public ArrayList<String> deliverEmail(String email) {
            try {
                return cache.get(email);
            } catch (ExecutionException e) {
                return bounce;
            }
        }

        @Override
        public ArrayList<String> load(String email) throws Exception {
            return delegate.deliverEmail(email);
        }

        @Override
        public HashMap<String, String> getEffectiveSettings(String email) {
            return delegate.getEffectiveSettings(email);
        }

        private final ArrayList<String> bounce;
        private final LoadingCache<String, ArrayList<String>> cache;
        private final Omp delegate;
    }
}
