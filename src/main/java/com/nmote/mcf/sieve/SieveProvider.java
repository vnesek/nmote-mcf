package com.nmote.mcf.sieve;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jsieve.SieveConfigurationException;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class SieveProvider implements Provider<Node> {

	@Inject
	public SieveProvider(SieveFactory sieveFactory, @Named("sieve") String sieveFile)
			throws SieveConfigurationException {
		this(sieveFactory, new File(sieveFile));
	}

	public SieveProvider(SieveFactory sieveFactory, @Named("sieve") File sieve) throws SieveConfigurationException {
		this.sieveFactory = sieveFactory;
		this.sieve = sieve;
	}

	public synchronized Node get() {
		long modified = sieve.lastModified();
		if (this.lastModified > System.currentTimeMillis() || node == null) {
			try {
				InputStream in = new BufferedInputStream(new FileInputStream(sieve), 4096);
				try {
					node = sieveFactory.parse(in);
					log.info("Loaded sieve '{}'", sieve);
					this.lastModified = modified;
				} finally {
					in.close();
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

	private static final Logger log = LoggerFactory.getLogger(SieveProvider.class);
}
