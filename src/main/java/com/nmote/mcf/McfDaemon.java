package com.nmote.mcf;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.daemon.support.DaemonLoader.Context;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class McfDaemon implements Daemon {

    public static void main(String[] args) throws Exception {
        Context ctx = new Context();
        ctx.setArguments(args);
        McfDaemon daemon = new McfDaemon();
        daemon.init(ctx);
        daemon.start();
    }

    @Override
    public void destroy() {
        log.debug("Destroyed");
    }

    @Override
    public void init(DaemonContext ctx) throws DaemonInitException, Exception {
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);

        Properties config = new Config().get();
        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new McfModule(config));
        for (String module : StringUtils.split(config.getProperty("modules", ""))) {
            log.info("Using {}", module);
            modules.add((AbstractModule) Class.forName(module).newInstance());
        }

        Injector injector = Guice.createInjector(modules);

        // Early initialization to report problems early on
        // log.debug("Using sieve {}", injector.getInstance(Node.class));
        // log.debug("Using queue {}", injector.getInstance(Queue.class));

        if (!"none".equals(config.getProperty("listen"))) {
            server = injector.getInstance(SMTPServer.class);
        }
        redelivery = injector.getInstance(Redelivery.class);
        counterUpdater = injector.getInstance(CounterUpdater.class);
        log.debug("Initialized");
    }

    @Override
    public void start() throws Exception {
        if (server != null) {
            server.start();
        }
        if (redelivery != null) {
            redelivery.start();
        }
        if (counterUpdater != null) {
            counterUpdater.start();
        }
        log.info("Started");
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (redelivery != null) {
            redelivery.stop();
        }
        if (counterUpdater != null) {
            counterUpdater.stop();
        }
        log.info("Stoped");
    }

    private final Logger log = LoggerFactory.getLogger(McfDaemon.class);
    private SMTPServer server;
    private Redelivery redelivery;
    private CounterUpdater counterUpdater;
}
