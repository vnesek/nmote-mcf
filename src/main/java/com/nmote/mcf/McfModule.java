package com.nmote.mcf;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.nmote.counters.Counters;
import com.nmote.counters.DefaultCounters;
import com.nmote.counters.NullCounters;
import org.apache.james.mime4j.stream.MimeConfig;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

import javax.inject.Singleton;
import java.util.Properties;
import java.util.function.Function;

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
        bind(BounceDeliveryAgent.class).in(Singleton.class);
        bind(DeliveryAgent.class).to(DefaultDeliveryAgent.class).in(Singleton.class);
        bind(Redelivery.class).in(Singleton.class);
        bind(new TypeLiteral<Function<String, String>>() {}).annotatedWith(SmtpRoutes.class).to(StaticSmtpRoutes.class).in(Singleton.class);

        // Setup mime config
        MimeConfig mc = new MimeConfig.Builder()
                .setMaxContentLen(Long.parseLong(config.getProperty("maxContentLen", "-1")))
                .setMaxHeaderCount(Integer.parseInt(config.getProperty("maxHeaderCount", "1000")))
                .setMaxHeaderLen(Integer.parseInt(config.getProperty("maxHeaderLen", "10000")))
                .setMaxLineLen(Integer.parseInt(config.getProperty("maxLineLen", "1000")))
                .build();
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
