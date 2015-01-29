package com.nmote.mcf;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config implements Provider<Properties> {

	protected static String getHostName() {
		String hostName;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
			int dot = hostName.indexOf('.');
			if (dot != -1) {
				hostName = hostName.substring(0, dot);
			}
		} catch (UnknownHostException e) {
			hostName = "localhost";
		}
		return hostName;
	}

	@Override
	public Properties get() {
		// Load default configuration
		Properties config = loadConfig("classpath:mcf-default.properties", null);

		// Override with config file (if any)
		config = loadConfig(System.getProperty("mcf.configFile", configFile), config);

		// Override with system properties
		for (Object key : Collections.list(System.getProperties().propertyNames())) {
			String k = (String) key;
			if (k.startsWith("mcf.")) {
				config.put(k.substring(4), System.getProperty((String) key));
			}
		}

		// Set host name
		if (config.getProperty("hostName") == null) {
			config.setProperty("hostName", getHostName());
		}

		// Set client helo host
		if (config.getProperty("clientHelo") == null) {
			config.setProperty("clientHelo", config.getProperty("hostName"));
		}

		// Dump configuration properties
		if (log.isDebugEnabled()) {
			@SuppressWarnings("unchecked")
			List<String> keys = (List<String>) Collections.list(config.propertyNames());
			Collections.sort(keys);
			for (String key : keys) {
				log.debug("Property {} = {}", key, config.getProperty(key));
			}
		}

		return config;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	protected Properties loadConfig(String fileName, Properties defaults) {
		Properties config = new Properties(defaults);
		if (fileName != null) {
			InputStream in;
			try {
				if (fileName.startsWith("classpath:")) {
					in = McfModule.class.getResourceAsStream(fileName.substring(10));
				} else {
					in = new FileInputStream(fileName);
				}
				config.load(in);
				in.close();
				log.info("Loaded configuration {}", fileName);
			} catch (Throwable t) {
				log.warn("Failed to load configuration {}: {}", fileName, t.getMessage());
			}
		}
		return config;
	}

	private String configFile = "mcf.properties";

	private final Logger log = LoggerFactory.getLogger(getClass());

}
