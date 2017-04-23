package com.nmote.mcf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class Redelivery implements Runnable {

    @Override
    public void run() {
        // Startup synchronization
        this.running = true;
        try {
            log.info("{} started", queue);
            synchronized (this) {
                this.notifyAll();
            }

            log.info("Redelivering every {} seconds", interval);
            int emptyRuns = 0;
            out:
            while (this.running) {
                try {
                    List<String> todo = queue.getMessageIds();
                    if (!todo.isEmpty()) {
                        emptyRuns = 0;
                        log.debug("Redelivering {} message(s)", todo.size());
                        for (String id : todo) {
                            if (!this.running) {
                                break out;
                            }
                            try {
                                MDC.put("message", id);
                                QueueMessage msg = queue.getMessage(id);
                                if (msg != null) {
                                    messageProcessor.redeliver(msg);
                                    if (msg.isCompleted()) {
                                        try {
                                            msg.delete();
                                            log.debug("Message redelivery completed");
                                        } catch (Exception e) {
                                            log.error("Failed to delete a message", e);
                                        }
                                    } else {
                                        try {
                                            msg.close();
                                            log.debug("Message requeued");
                                        } catch (Exception e) {
                                            log.error("Failed to requeue a message", e);
                                        }
                                    }
                                }
                            } catch (Throwable t) {
                                log.error("Failed to redeliver", t);
                            } finally {
                                MDC.remove("message");
                            }
                        }
                    } else {
                        // Nothing to redeliver
                        ++emptyRuns;
                        if ((emptyRuns % 10) == 0) {
                            log.debug("{} is empty", queue);
                        }
                    }

                    // Wait for next redelivery run
                    synchronized (this) {
                        if (this.interval <= 0) {
                            break out;
                        }
                        this.wait(interval * 1000);
                    }
                } catch (Throwable t) {
                    log.error("Failed to list a queue", t);
                }
            }
        } finally {
            // We're busted
            this.running = false;
            log.info("{} stopped", queue);
        }
    }

    @Inject
    public void setInterval(@Named("redeliveryInterval") int interval) {
        this.interval = interval;
    }

    public synchronized void start() {
        if (interval <= 0) {
            log.info("Redelivery is disabled (redeliveryInterval = {}, to enable set to value grater than zero)",
                    interval);
            return;
        }

        if (this.running) {
            log.info("Redelivery is already started");
            return;
        }

        // Launch a worker thread
        Thread t = new Thread(this);
        t.setName("Redelivery " + interval + "s");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
        log.info("{} starting", queue);

        // Wait for it to start running
        while (!this.running) {
            try {
                this.wait(100);
            } catch (InterruptedException e) {
                this.running = false;
                throw new RuntimeException("start interrupted", e);
            }
        }
    }

    public synchronized void stop() {
        if (this.running) {
            this.running = false;
            this.notifyAll();
            log.info("{} stopping", queue);
        }
    }

    private transient boolean running;

    private transient int interval = 30;

    @Inject
    private Queue queue;

    @Inject
    private MessageProcessor messageProcessor;

    private Logger log = LoggerFactory.getLogger(getClass());
}
