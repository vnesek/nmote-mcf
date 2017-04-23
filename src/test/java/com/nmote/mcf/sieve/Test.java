package com.nmote.mcf.sieve;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nmote.mcf.McfModule;
import com.nmote.mcf.MessageProcessor;
import com.nmote.mcf.Queue;
import com.nmote.mcf.QueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Test {

    private static final Logger log = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) throws Exception {
        for (int j = 0; j < 1; ++j) {
            Injector injector = Guice.createInjector(new McfModule(), new Module());


            Queue queue = injector.getInstance(Queue.class);
            System.out.println(queue);

            log.info("Starting evaluations");
            SieveMessageProcessor processor = injector.getInstance(SieveMessageProcessor.class);
            int N = 1;
            int count = 0;
            long start = System.currentTimeMillis();
            for (int i = 0; i < N; ++i) {
                for (String msgId : queue.getMessageIds()) {
                    QueueMessage m = queue.getMessage(msgId);
                    // log.debug("Header {}", m.getHeader());
                    // HeaderRewriter.writeTo(m.getHeader(), System.err);
                    System.err.println(m.getHeader());
                    processor.deliver(m);
                    ++count;
                    // System.out.println(msgId + " -> " + a.getActions() +
                    // " Size: " + m.getIndex().getMemorySize());
                    // mboxDelivery.deliver(m);
                    if (count >= 10000) {
                        break;
                    }
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info("Processed {} messages", count);
            log.info("Finished {} evals/sec", (count * 1000.0 / elapsed));
        }
    }

    public static void main3(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new McfModule(), new Module());

        Queue queue = injector.getInstance(Queue.class);
        System.out.println(queue);

        log.info("Starting evaluations");
        MessageProcessor processor = injector.getInstance(MessageProcessor.class);
        int N = 100;
        int count = 0;
        long start = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            for (String msgId : queue.getMessageIds()) {
                QueueMessage m = queue.getMessage(msgId);
                MDC.put("message", msgId);
                processor.check(m);
                processor.route(m);
                processor.deliver(m);
                MDC.remove("message");
                ++count;
                // System.out.println(msgId + " -> " + a.getActions() +
                // " Size: " + m.getIndex().getMemorySize());
                // mboxDelivery.deliver(m);
                if (count >= 10000) {
                    break;
                }
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("Processed {} messages", count);
        log.info("Finished {} evals/sec", (count * 1000.0 / elapsed));
    }
}
