package com.nmote.mcf;

import com.nmote.counters.Counters;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.client.SMTPClient.Response;
import org.subethamail.smtp.client.SMTPException;
import org.subethamail.smtp.client.SmartClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class SmtpDeliveryAgent implements DeliveryAgent {

    public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
        String destination = delivery.getDestination();
        if (destination.startsWith("remote:")) {
            String domain = StringUtils.substringAfterLast(destination, "@");
            destination = smtpRoutes.apply(domain);
            if (!destination.startsWith("smtp:")) {
                log.info("Routed {} => {}, can't deliver", domain, destination);
                delivery.setStatus("cant-deliver (" + destination + ")");
                delivery.setCompleted();
                return;
            }
        }

        String host;
        int port;
        { // Parse host and port
            String[] a = StringUtils.split(destination, ':');
            host = a[1];
            port = a.length > 2 ? Integer.parseInt(a[2]) : 25;
        }

        final String key = "smtp." + host + "." + port;
        final Logger log = LoggerFactory.getLogger(key);
        final SmartClient smartClient = new SmartClient(host, port, heloHost);
        int written = 0;

        try {
            // Send envelop from
            log.debug("from {}", msg.getFrom());
            smartClient.from(msg.getFrom());

            // Send envelope recipients
            for (String to : delivery.getRecipients()) {
                log.debug("to {}", to);
                smartClient.to(to);
            }

            // Start data
            log.debug("data start");
            smartClient.dataStart();

            // Copy data
            byte[] buffer = new byte[bufferSize];
            InputStream in = msg.getInputStream();
            try {
                // Write data to SMTP server
                int bytes;
                while ((bytes = in.read(buffer)) != -1) {
                    smartClient.dataWrite(buffer, bytes);
                    written += bytes;
                }

                // Finish data command
                Response dataEnd = smartClient.dataEnd();
                delivery.setStatus("pass=>" + delivery.getDestination() + " (" + dataEnd.getMessage() + ')');
                delivery.setCompleted();
                log.info("Delivered {}, written {} bytes, {}", msg.getId(), written, dataEnd);
            } finally {
                IOUtils.closeQuietly(in);

                // Increase transfer counter regardless of delivery status
                counters.add("bytes." + key, written);
            }

            // Increase number of delivered messages
            counters.add("count." + key, 1);
        } catch (SMTPException e) {
            Response response = e.getResponse();

            // Check if error is in a list of SMTP reject codes. If so,
            // we'll complete this without re-delivery
            if (smtpRejectCodes.contains(response.getCode())) {
                delivery.setStatus("pass-reject-" + response.getCode() + " " + delivery.getDestination() + " ("
                        + response.getMessage() + ')');
                counters.add("remote.reject." + response.getCode(), 1);
                delivery.setCompleted();

                // Make a copy of message
                if (!"trash".equals(smtpRejectTo)) {
                    Delivery copy = new Delivery();
                    copy.setDestination(smtpRejectTo);
                    copy.setRecipients(delivery.getRecipients());
                    msg.getDeliveries().add(copy);
                }
                log.warn("Rejected {} into {}, written {} bytes, {}", msg.getId(), smtpRejectTo, written, response);
            } else {
                // Force re-delivery, re-throw original exception
                throw e;
            }
        } finally {
            smartClient.close();
        }
    }

    @Inject
    public void setSmtpRejectCodes(@Named("smtpRejectCodes") String codes) {
        smtpRejectCodes = new HashSet<>();
        for (String s : StringUtils.split(codes)) {
            smtpRejectCodes.add(Integer.parseInt(s));
        }
    }

    @Inject
    @SmtpRoutes
    private Function<String, String> smtpRoutes;

    @Inject
    @Named("clientBufferSize")
    private int bufferSize = 1024;

    @Inject
    @Named("smtpRejectTo")
    private String smtpRejectTo = "trash";

    @Inject
    @Named("clientHelo")
    private String heloHost;

    @Inject
    private Counters counters;

    private Set<Integer> smtpRejectCodes;

    private final Logger log = LoggerFactory.getLogger(SmtpDeliveryAgent.class);
}
