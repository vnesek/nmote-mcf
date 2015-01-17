package com.nmote.mcf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nmote.counters.Counters;

public class MBoxDeliveryAgent implements DeliveryAgent {

	private static final Set<File> lockedFiles = new HashSet<File>();

	private static String formatAsctime(Date date) {
		String s = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy").format(date);
		if (s.charAt(8) == '0') {
			s = s.substring(0, 8) + ' ' + s.substring(9);
		}
		return s;
	}

	@Override
	public void deliver(QueueMessage msg, Delivery delivery) throws IOException {
		final File mbox = new File(delivery.getDestination().substring(5));
		final String mboxPath = mbox.getAbsolutePath();
		final String key = mboxPath.replace(File.separatorChar, '.');
		final Logger log = LoggerFactory.getLogger("mbox" + key);

		// Internal in-JVM lock
		synchronized (lockedFiles) {
			while (lockedFiles.contains(mbox)) {
				if (log.isDebugEnabled()) {
					log.debug("MBox is locked internally");
				}
				try {
					lockedFiles.wait();
				} catch (InterruptedException e) {
					throw new IOException("interrupted while waiting on a file lock");
				}
			}
			lockedFiles.add(mbox);
		}
		try {
			try {
				FileOutputStream fileOut = new FileOutputStream(mbox, true);
				FileChannel channel = fileOut.getChannel();
				// External file lock
				for (int i = 0; i < lockTries; ++i) {
					FileLock lock = channel.tryLock();
					if (lock == null) {
						log.info("MBox is locked, retrying in 1sec");
						Thread.sleep(1000);
						continue;
					}
					if (channel.size() > 0) {
						fileOut.write('\n');
					}
					// Write From line
					StringBuilder from = new StringBuilder();
					from.append("From ");
					from.append(StringUtils.replaceChars(msg.getFrom(), " \t\r\n", "____"));
					from.append(' ');
					from.append(formatAsctime(new Date()));
					if (appendIdToFromLine) {
						from.append(" id=").append(msg.getId());
					}
					from.append('\n');
					fileOut.write(from.toString().getBytes("utf-8"));

					long written = msg.writeTo(
							new MBoxOutputStream(new BufferedOutputStream(fileOut, outputBufferSize)),
							this.keepHeaderIntact);
					log.info("Delivered {}, written {} bytes", msg.getId(), written);

					counters.add("bytes.mbox" + key, written);
					counters.add("count.mbox" + key, 1);

					delivery.setStatus(mboxPath);
					delivery.setCompleted();
					return;
				}
				fileOut.close();
				throw new IOException("failed to lock mbox after " + lockTries + " lockTries");
			} catch (InterruptedException e) {
				throw new IOException("interrupted while waiting for unlock");
			}
		} finally {
			// Unlock internal
			synchronized (lockedFiles) {
				lockedFiles.remove(mbox);
				lockedFiles.notifyAll();
			}
		}
	}

	@Inject
	public void setAppendIdToFromLine(@Named("appendIdToFromLine") boolean addMoreinfo) {
		this.appendIdToFromLine = addMoreinfo;
	}

	public void setKeepHeaderIntact(boolean keepHeaderIntact) {
		this.keepHeaderIntact = keepHeaderIntact;
	}

	public void setOutputBufferSize(int outputBufferSize) {
		this.outputBufferSize = outputBufferSize;
	}

	private boolean keepHeaderIntact = true;

	private int outputBufferSize = 4 * 1024;

	private int lockTries = 10;

	private boolean appendIdToFromLine = true;

	@Inject
	private Counters counters;

}
