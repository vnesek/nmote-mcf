package com.nmote.mcf.tests;

import org.apache.commons.io.IOUtils;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.wiser.Wiser;

import java.io.IOException;
import java.io.InputStream;

public class SmtpRejectCodesTest {

    public static void main(String[] args) {
        Wiser wiser = new Wiser() {
            public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException,
                    IOException {
                System.err.println("Rejecting");
                IOUtils.toByteArray(data);
                throw new RejectException(467, "test reject");
            }

            ;
        };
        wiser.setHostname("localhost");
        wiser.setPort(8029);
        wiser.start();

    }
}
