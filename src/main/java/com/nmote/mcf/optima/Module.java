package com.nmote.mcf.optima;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.nmote.mcf.MessageProcessor;

public class Module extends AbstractModule {

	protected void configure() {
		bind(MessageProcessor.class).to(OptimaMessageProcessor.class).in(Singleton.class);
		bind(MaildirSource.class).to(VPopMailMaildirSource3.class).in(Singleton.class);
	};
}
