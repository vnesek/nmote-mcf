package com.nmote.mcf;

import com.nmote.maildir.DotQmail;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class DotQmailMessageProcessor extends DefaultMessageProcessor {

    @Override
    public void route(QueueMessage message) throws IOException {
        // Check existing maildir deliveries for .qmail files
        for (final Delivery d : new ArrayList<>(message.getDeliveries())) {
            String dest = d.getDestination();
            if (dest.startsWith("maildir:")) {
                File maildir = new File(dest.substring(8));
                File dotQmail = new File(maildir.getParentFile(), ".qmail");

                if (!dotQmail.isFile()) {
                    // Not a file...
                    continue;
                }

                if (!dotQmail.canRead()) {
                    log.debug("Not readable, skipping {}", dotQmail);
                    continue;
                }

                log.debug("Processing {}", dotQmail);
                InputStream in = new FileInputStream(dotQmail);
                DotQmail dq;
                try {
                    dq = new DotQmail().load(in);
                } finally {
                    IOUtils.closeQuietly(in);
                }

                // Local maildir deliveries
                for (String a : dq.maildirLines()) {
                    log.info("Local maildir {}", a);
                    message.deliverTo(d.getRecipients(), "maildir:" + a);
                }

                // Local mailbox deliveries
                for (String a : dq.mailboxLines()) {
                    log.info("Local mailbox {}", a);
                    message.deliverTo(d.getRecipients(), "mbox:" + a);
                }

                // Remote forward over SMTP
                for (String a : dq.forwardLines()) {
                    log.info("Remote forward {}", a);
                    message.deliverTo(a, "forward");
                }

                d.setStatus("dotqmail=>" + maildir.getParentFile().getName());
                d.setCompleted();
            }
        }
    }

    private final Logger log = LoggerFactory.getLogger(getClass());
}
