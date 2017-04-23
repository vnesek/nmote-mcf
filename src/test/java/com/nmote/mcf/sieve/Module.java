package com.nmote.mcf.sieve;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.nmote.mcf.DefaultMessageProcessor;
import com.nmote.mcf.MessageProcessor;
import org.apache.jsieve.ConfigurationManager;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.parser.generated.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(MessageProcessor.class).to(DefaultMessageProcessor.class);
        bind(Node.class).toProvider(SieveProvider.class).in(Singleton.class);
        bind(LuceneQueryParser.class).toInstance(new CachingLuceneQueryParser(new DefaultLuceneQueryParser()));
        bind(SieveFactory.class).toProvider(new Provider<SieveFactory>() {
            public SieveFactory get() {
                return cm.build();
            }

            @Inject
            ConfigurationManager cm;
        }).in(Singleton.class);
    }

}
