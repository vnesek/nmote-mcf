package com.nmote.mcf.clamav;

import com.google.inject.Inject;
import com.nmote.mcf.DefaultMessageProcessor;
import com.nmote.mcf.QueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.io.IOException;

public class ClamAVMessageProcessor extends DefaultMessageProcessor {

    @Override
    public void check(QueueMessage message) throws IOException {
        if (!message.getFlags().contains(QueueMessage.Flags.VIRUS)) {
            long start = System.currentTimeMillis();

            try (ClamAVClient cavc = client.get()) {
                String result = cavc.instream(message.getInputStream());
                long elapsed = System.currentTimeMillis() - start;
                if (result != null) {
                    log.debug("Detected virus {} in {} ms", result, elapsed);
                    message.getHeader().add("X-MCF-ClamAV", result);
                    message.getFlags().add(QueueMessage.Flags.VIRUS);
                } else {
                    log.debug("Checked for viruses in {} ms", elapsed);
                }
            }
        }
    }

    @Inject
    private Provider<ClamAVClient> client;

    private Logger log = LoggerFactory.getLogger(getClass());
}
