package com.nmote.mcf;

import com.nmote.counters.Counters;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.io.CRLFOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MaildirDeliveryAgent implements DeliveryAgent {

    @Inject
    private Counters counters;
    private int outputBufferSize = 4 * 1024;
    private boolean keepHeaderIntact = true;
    private boolean autoCreate;
    private boolean maildirSize;
    private Random random = new Random();
    private int sequence;
    private String hostName;
    private String fileNameFormat = "%2$d.%5$s.%3$s.%4$s%6$03X";

    @Override
    public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
        final File maildir = new File(delivery.getDestination().substring(8));
        final String key = maildir.getAbsolutePath().replace(File.separatorChar, '.');
        final Logger log = LoggerFactory.getLogger("maildir" + key);

        if (!maildir.exists() && autoCreate) {
            if (maildir.mkdirs()) {
                log.info("Created maildir: {}", maildir);
            } else {
                log.error("Failed to create maildir: {}", maildir);
            }
        }

        if (!maildir.isDirectory()) {
            throw new IOException("not a directory: " + maildir);
        }

        if (!maildir.canWrite()) {
            throw new IOException("not writeable: " + maildir);
        }

        // Ensure there is a 'tmp'
        File tmp = new File(maildir, "tmp");
        if (!tmp.exists()) {
            if (!tmp.mkdir()) {
                throw new IOException("can't create maildir tmp directory: " + maildir);
            }
        }

        // Create unique file in 'tmp'
        File tmpFile = null;
        for (int i = 0; i < 10; ++i) {
            int seq;
            synchronized (this) {
                seq = ++sequence;
            }
            // Old message id format
            // String unique = Long.toHexString(System.currentTimeMillis()) + '.' + hostName + '.' + seq + '.'
            //        + msg.getId() + '.' + Integer.toHexString(random.nextInt(0xFFF));
            long now = System.currentTimeMillis();
            String unique = String.format(fileNameFormat,
                    now, // 1
                    now / 1000, // 2
                    hostName, // 3
                    seq, // 4
                    msg.getId(), // 5
                    random.nextInt(0xFFF) // 6
            );

            tmpFile = new File(tmp, unique);
            if (tmpFile.createNewFile()) {
                break;
            } else {
                tmpFile = null;
            }
            long toSleep = new Random().nextInt(500);
            try {
                Thread.sleep(toSleep);
            } catch (InterruptedException e) {
                throw new IOException("interrupted while creating unique filename");
            }
        }
        if (tmpFile == null) {
            throw new IOException("failed to create unique filename");
        }

        // Copy message to 'tmp'
        OutputStream out = new CRLFOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile),
                outputBufferSize));
        long written = msg.writeTo(out, keepHeaderIntact);
        log.info("Saved to tmp {}, written {} bytes", msg.getId(), written);

        counters.add("bytes.maildir" + key, written);
        counters.add("count.maildir" + key, 1);

        // Ensure there is a 'new' directory
        File nu = new File(maildir, "new");
        if (!nu.exists()) {
            if (!nu.mkdir()) {
                throw new IOException("can't create maildir new directory: " + maildir);
            }
        }

        // Move from 'tmp' to 'new'
        File newFile = new File(nu, tmpFile.getName() + ",S=" + written);
        if (!tmpFile.renameTo(newFile)) {
            tmpFile.delete();
            throw new IOException("can't move tmp file to new directory: " + tmpFile);
        }

        // Update maildir size
        if (maildirSize) {
            File mds = new File(maildir, "maildirsize");
            if (mds.exists() && mds.canWrite()) {
                FileUtils.write(mds, written + " " + 1 + "\n", StandardCharsets.ISO_8859_1, true);
            }
        }

        delivery.setStatus("maildir=>" + tmpFile.getName());
        //delivery.setStatus("pass-reject-5302 buzz off!");
        delivery.setCompleted();
        log.info("Delivered {} to {}", msg.getId(), delivery.getStatus());
    }

    @Inject
    public void setHostName(@Named("hostName") String hostname) {
        this.hostName = hostname;
    }

    @Inject
    public void setAutoCreate(@Named("maildirAutoCreate") boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    @Inject
    public void setMaildirSize(@Named("maildirSize") boolean maildirSize) {
        this.maildirSize = maildirSize;
    }

    @Inject
    public void setKeepHeaderIntact(@Named("originalHeaders") boolean keepHeaderIntact) {
        this.keepHeaderIntact = keepHeaderIntact;
    }

    @Inject
    public void setFileNameFormat(@Named("fileNameFormat") String fileNameFormat) {
        this.fileNameFormat = fileNameFormat;
    }

    public void setOutputBufferSize(int outputBufferSize) {
        this.outputBufferSize = outputBufferSize;
    }
}
