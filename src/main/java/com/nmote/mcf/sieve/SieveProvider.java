package com.nmote.mcf.sieve;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.jsieve.SieveConfigurationException;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class SieveProvider implements Provider<Node> {

    private static final Logger log = LoggerFactory.getLogger(SieveProvider.class);

    @Inject
    public SieveProvider(SieveFactory sieveFactory, @Named("sieve") String sieveFile)
            throws SieveConfigurationException {
        this(sieveFactory, new File(sieveFile));
    }

    public SieveProvider(SieveFactory sieveFactory, @Named("sieve") File sieve) {
        this.sieveFactory = sieveFactory;
        this.sieve = sieve;
    }

    public synchronized Node get() {
        long modified = sieve.lastModified();
        if (this.lastModified > System.currentTimeMillis() || node == null) {
            try {
                try (InputStream in = new BufferedInputStream(new FileInputStream(sieve), 4096)) {
                    node = sieveFactory.parse(in);
                    log.info("Loaded sieve '{}'", sieve);
                    this.lastModified = modified;
                }
            } catch (FileNotFoundException e) {
                if (node == null) {
                    throw new RuntimeException("sieve file not found " + sieve);
                } else {
                    log.error("Sieve file not found '{}' using old one", sieve);
                }
            } catch (IOException e) {
                if (node == null) {
                    throw new RuntimeException("failed to load sieve " + sieve);
                } else {
                    log.error("Failed to load sieve '{}' using old one", sieve);
                }
            } catch (ParseException e) {
                if (node == null) {
                    throw new RuntimeException("failed to parse sieve " + sieve + ": " + e);
                } else {
                    log.error("Failed to parse sieve '{}' using old one", sieve);
                }
            }
        }
        return node;
    }

    private File sieve;
    private Node node;
    private long lastModified;
    private SieveFactory sieveFactory;
}
