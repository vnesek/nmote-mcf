package com.nmote.mcf.sieve;

import com.google.inject.name.Named;
import com.nmote.counters.Counters;
import com.nmote.mcf.DefaultMessageProcessor;
import com.nmote.mcf.QueueMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.*;
import org.apache.jsieve.parser.generated.Node;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Routes messages according to sieve rules from a sieve configuration file.
 */
public class SieveMessageProcessor extends DefaultMessageProcessor {

    @Override
    public void route(final QueueMessage message) throws IOException {
        // Evaluate sieve for each envelope recipient in turn
        for (final String recipient : message.getRecipients()) {
            long start = System.currentTimeMillis();
            MDC.put("to", recipient);
            try {
                // Evaluate message against a sieve
                MailAdapter mailAdapter = new QueueMessageMailAdapter(message) {
                    public void executeActions() throws SieveException {
                        if (log.isDebugEnabled()) {
                            log.debug("Executing actions {}", getActions());
                        }
                        for (Action action : getActions()) {
                            if (action instanceof ActionFileInto) {
                                String destination = ((ActionFileInto) action).getDestination();
                                message.deliverTo(recipient, destination);
                                log.info("Routed to {}", destination);
                            } else if (action instanceof ActionKeep) {
                                if (keep != null) {
                                    for (String k : keep) {
                                        message.deliverTo(recipient, k);
                                        log.info("Keeping {}", k);
                                    }
                                } else {
                                    log.error("Keep action not configured");
                                }
                            }
                        }
                    }

                    @Override
                    public List<String> getEnvelope(String name) throws SieveMailException {
                        if (name.equalsIgnoreCase("to")) {
                            return Collections.singletonList(recipient);
                        } else {
                            return super.getEnvelope(name);
                        }
                    }

                    public boolean isInBodyText(String phraseCaseInsensitive) throws SieveMailException {
                        try {
                            Query query = queryParser.parse(phraseCaseInsensitive);
                            float score = msg.getIndex().search(query);
                            log.debug("Lucene query '{}' score {}",
                                    new Object[]{StringUtils.abbreviate(phraseCaseInsensitive, 20), score});
                            return score > 0;
                        } catch (Exception e) {
                            throw new SieveMailException(e);
                        }
                    }
                };

                try {
                    log.debug("Sieve evaluating");
                    sieveFactory.evaluate(mailAdapter, sieve.get());
                } catch (SieveException e) {
                    // Sieve evaluation failed miserably...
                    log.error("Sieve evaluation failed, keeping a message", e);
                    mailAdapter.addAction(new ActionKeep());
                }
            } finally {
                MDC.remove("to");
                long elapsed = System.currentTimeMillis() - start;
                counters.add("time.sieve", elapsed);
                counters.add("count.sieve", 1);
            }
        }
    }

    @Inject
    public void setKeep(@Named("keep") String keep) {
        this.keep = StringUtils.split(keep);
    }

    @Inject
    private SieveFactory sieveFactory;

    @Inject
    private Provider<Node> sieve;

    @Inject
    private LuceneQueryParser queryParser;

    @Inject
    private Counters counters;

    private String[] keep;

    private Logger log = LoggerFactory.getLogger(getClass());
}
