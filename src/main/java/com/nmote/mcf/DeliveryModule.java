package com.nmote.mcf;

import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class DeliveryModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MessageProcessor.class).to(DeliveryMessageProcessor.class).in(Singleton.class);
    }
}
