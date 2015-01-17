package com.nmote.mcf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Named;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.james.mime4j.stream.MimeConfig;

import com.google.inject.Inject;

public class Queue {

	public enum Format {
		NATIVE, MAILDIR
	}

	public Queue(File queueDir) {
		this(queueDir, new MimeConfig());
	}

	public Queue(File queueDir, MimeConfig mimeConfig) {
		if (!queueDir.exists()) {
			throw new IllegalArgumentException("queue directory  " + queueDir.getAbsolutePath() + " doesn't exists");
		}
		if (!queueDir.isDirectory()) {
			throw new IllegalArgumentException("queue directory  " + queueDir.getAbsolutePath() + " is not a directory");
		}
		if (!queueDir.canWrite()) {
			throw new IllegalArgumentException("queue directory  " + queueDir.getAbsolutePath() + " is not writtable");
		}
		if (!queueDir.canRead()) {
			throw new IllegalArgumentException("queue directory  " + queueDir.getAbsolutePath() + " is not readable");
		}
		this.queueDir = queueDir;
		this.random = new Random();
		this.mimeConfig = mimeConfig;
	}

	@Inject
	public Queue(@Named("queueDir") String queueDir, MimeConfig mimeConfig) {
		this(new File(queueDir), mimeConfig);
	}

	public Format getFormat() {
		return format;
	}

	public QueueMessage getMessage(String id) throws IOException {
		File message = new File(queueDir, id);
		if (!message.exists() || !message.canRead()) {
			return null;
		} else {
			return new QueueMessage(message, mimeConfig);
		}
	}

	public List<String> getMessageIds() throws IOException {
		List<String> result = new ArrayList<String>();
		File[] files = queueDir.listFiles();
		if (files != null) {
			for (File file : files) {
				String name = file.getName();
				if (format.equals(Format.NATIVE)) {
					if (!file.isHidden() && !name.startsWith(".") && name.endsWith(".json")) {
						result.add(name.substring(0, name.length() - 5));
					}
				} else {
					if (!file.isHidden() && !name.startsWith(".") && name.endsWith(".json") && !file.isDirectory()) {
						result.add(name);
					}
				}
			}
		}
		return result;
	}

	public File getQueueDir() {
		return queueDir;
	}

	public QueueMessage newMessage() throws IOException {
		return new QueueMessage(newMessageFile(), mimeConfig);
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("queueDir", queueDir);
		return b.toString();
	}

	protected String randomMessageId(int len) {
		final int z1 = 'z' - 'a', z2 = z1 * 2, z3 = z2 + 10;
		char[] a = new char[len];
		for (int i = 0; i < len; ++i) {
			int r = random.nextInt(z3);
			if (r < z1) {
				a[i] = (char) ('a' + r);
			} else if (r < z2) {
				a[i] = (char) ('A' + r - z1);
			} else {
				a[i] = (char) ('0' + r - z2);
			}
		}
		return new String(a);
	}

	private File newMessageFile() {
		File file = null;
		for (int i = 0; i < 7; ++i) {
			String id = randomMessageId(8 + i);
			file = new File(queueDir, id);
			if (!file.exists()) {
				break;
			}
		}
		if (file == null) {
			throw new AssertionError("all message id's are exhausted");
		}
		return file;
	}

	private MimeConfig mimeConfig;

	private Random random;

	private File queueDir;

	private Format format = Format.NATIVE;
}
