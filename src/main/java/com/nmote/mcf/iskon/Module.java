package com.nmote.mcf.iskon;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.nmote.mcf.CompositeMessageProcessor;
import com.nmote.mcf.DeliveryMessageProcessor;
import com.nmote.mcf.MessageProcessor;
import com.nmote.mcf.sieve.SieveMessageProcessor;

import javax.inject.Inject;
import javax.inject.Singleton;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(MessageProcessor.class).toProvider(new Provider<MessageProcessor>() {

            @Override
            public MessageProcessor get() {
                return new CompositeMessageProcessor(sieve, delivery);
            }

            @Inject
            DeliveryMessageProcessor delivery;

            @Inject
            SieveMessageProcessor sieve;
        }).in(Singleton.class);
    }

}
