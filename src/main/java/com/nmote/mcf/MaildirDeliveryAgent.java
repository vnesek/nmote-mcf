package com.nmote.mcf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.io.CRLFOutputStream;

import com.nmote.counters.Counters;

public class MaildirDeliveryAgent implements DeliveryAgent {

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
			String unique = Long.toHexString(System.currentTimeMillis()) + '.' + hostName + '.' + seq + '.'
					+ msg.getId() + '.' + Integer.toHexString(random.nextInt(0xFFF));
			tmpFile = new File(tmp, unique);
			if (tmpFile.createNewFile()) {
				break;
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

	public void setKeepHeaderIntact(boolean keepHeaderIntact) {
		this.keepHeaderIntact = keepHeaderIntact;
	}

	public void setOutputBufferSize(int outputBufferSize) {
		this.outputBufferSize = outputBufferSize;
	}

	@Inject
	private Counters counters;

	private int outputBufferSize = 4 * 1024;
	private boolean keepHeaderIntact = true;
	private boolean autoCreate;
	private Random random = new Random();
	private int sequence;
	private String hostName;
}
