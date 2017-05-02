package com.nmote.mcf;

import com.nmote.counters.Counters;
import com.nmote.counters.NullCounters;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CounterUpdater implements Runnable {

    @Override
    public void run() {
        // Startup synchronization
        this.running = true;
        try {
            log.debug("Started");
            synchronized (this) {
                this.notifyAll();
            }

            while (this.running) {
                try {
                    // Wait for next run
                    synchronized (this) {
                        if (this.interval <= 0) {
                            break;
                        }
                        this.wait(interval * 1000);
                    }

                    // Save counters
                    saveCounters(counters, countersFile);
                } catch (Throwable t) {
                    log.error("Failed to sync counters file {}", countersFile, t);
                }
            }
        } finally {
            // We're busted
            this.running = false;
            log.debug("Stopped");
        }
    }

    public void saveCounters(Counters counters, File file) throws IOException {
        // Open tmp file
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        Writer out = new BufferedWriter(new FileWriter(tmp));
        out.write("# MCF Counters " + new Date() + "\n");

        long[] intervals = {1, 10, 30};

        for (int i = 0; i < intervals.length; ++i) {
            long interval = intervals[i] * DateUtils.MILLIS_PER_MINUTE;

            boolean last = i == intervals.length - 1;

            // Format counters in a human readable format
            long time = System.currentTimeMillis() - interval;
            SortedMap<String, SortedMap<String, Number>> stats = new TreeMap<>();
            for (String c : counters.counters()) {
                long v = last ? counters.collapse(c, time) : counters.value(c, time);
                int idx = c.indexOf('.');
                String k1 = c.substring(idx + 1);
                String k2 = c.substring(0, idx);
                SortedMap<String, Number> e = stats.get(k1);
                if (e == null) {
                    e = new TreeMap<>();
                    stats.put(k1, e);
                }
                e.put(k2, v);
            }

            // Calculate averages
            if (!last) {
                for (Map.Entry<String, SortedMap<String, Number>> e : stats.entrySet()) {
                    SortedMap<String, Number> s = e.getValue();
                    Long count = (Long) s.get("count");
                    if (count != null) {
                        s.put("msg/s", count * 1000.0 / interval);
                        Long timez = (Long) s.get("time");
                        if (timez != null) {
                            s.put("ms/msg", timez * 1F / count);
                        }
                    }
                    Long bytes = (Long) s.get("bytes");
                    if (bytes != null) {
                        s.put("bytes/s", bytes * 1000.0 / interval);
                    }
                }
            }

            // Write out status
            out.write(last ? "\n# Since start\n" : ("\n# Last  " + intervals[i] + " minute(s)\n"));
            for (Map.Entry<String, SortedMap<String, Number>> e : stats.entrySet()) {
                out.write(e.getKey());
                for (Map.Entry<String, Number> e2 : e.getValue().entrySet()) {
                    out.write("\n\t");
                    out.write(e2.getValue().toString());
                    out.write(' ');
                    out.write(e2.getKey());
                }
                out.write('\n');
            }
        }
        out.close();
        if (!tmp.renameTo(file)) {
            throw new IOException("failed to move " + tmp + " to " + file);
        }
    }

    @Inject
    public void setCounters(Counters counters) {
        this.counters = counters;
    }

    public void setCountersFile(File countersFile) {
        this.countersFile = countersFile;
    }

    @Inject
    public void setCountersFile(@Named("countersFile") String countersFile) {
        setCountersFile(new File(countersFile));
    }

    @Inject
    public void setInterval(@Named("countersInterval") int interval) {
        if (interval > 120) {
            interval = 120;
        }
        this.interval = interval;
    }

    public synchronized void start() {
        if (counters instanceof NullCounters || countersFile == null || interval <= 0) {
            log.info("Counters disabled");
            return;
        }

        if (this.running) {
            log.debug("Counter updater is already started");
            return;
        }

        // Launch a worker thread
        Thread t = new Thread(this);
        t.setName("CounterUpdater");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
        log.debug("{} starting", countersFile);

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
            log.info("Stopping");
        }
    }

    private Counters counters;
    private File countersFile;
    private transient int interval = 20;
    private Logger log = LoggerFactory.getLogger(getClass());
    private transient boolean running;
}
