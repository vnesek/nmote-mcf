package com.nmote.mcf;

import java.util.Properties;

import javax.inject.Singleton;

import org.apache.james.mime4j.stream.MimeConfig;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.nmote.counters.Counters;
import com.nmote.counters.DefaultCounters;
import com.nmote.counters.NullCounters;

public class McfModule extends AbstractModule {

	public McfModule() {
		this(new Config().get());
	}

	public McfModule(Properties config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		// Bind properties as constants
		Names.bindProperties(this.binder(), config);

		bind(MessageHandlerFactory.class).to(McfMessageHandlerFactory.class).in(Singleton.class);
		bind(SMTPServer.class).to(McfSmtpServer.class).in(Singleton.class);
		bind(SmtpDeliveryAgent.class).in(Singleton.class);
		bind(MBoxDeliveryAgent.class).in(Singleton.class);
		bind(MaildirDeliveryAgent.class).in(Singleton.class);
		bind(DiscardDeliveryAgent.class).in(Singleton.class);
		bind(DeliveryAgent.class).to(DefaultDeliveryAgent.class).in(Singleton.class);
		bind(Redelivery.class).in(Singleton.class);

		// Setup mime config
		MimeConfig mc = new MimeConfig();
		mc.setMaxContentLen(Long.parseLong(config.getProperty("maxContentLen", "-1")));
		mc.setMaxHeaderCount(Integer.parseInt(config.getProperty("maxHeaderCount", "1000")));
		mc.setMaxHeaderLen(Integer.parseInt(config.getProperty("maxHeaderLen", "10000")));
		mc.setMaxLineLen(Integer.parseInt(config.getProperty("maxLineLen", "1000")));
		bind(MimeConfig.class).toInstance(mc);

		// Setup counters
		if (config.getProperty("countersFile") != null
				&& Integer.parseInt(config.getProperty("countersInterval", "-1")) > 0) {
			bind(Counters.class).to(DefaultCounters.class).in(Singleton.class);
		} else {
			bind(Counters.class).to(NullCounters.class).in(Singleton.class);
		}
	}

	private Properties config;
}
