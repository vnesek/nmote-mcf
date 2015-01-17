package com.nmote.mcf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmote.io.DeferredFileOutputStream;
import com.nmote.mcf.Header.HeaderField;

public class QueueMessage {

	public interface Flags {
		String SPAM = "spam";
		String VIRUS = "virus";
	}

	private static ObjectMapper MAPPER = new ObjectMapper();

	static {
		//MAPPER.configure(MapperFeature.INDENT_OUTPUT, true);
	}

	public static void main(String[] args) throws Exception {
		File f = new File("/tmp/fff");
		f.delete();
		QueueMessage message = new QueueMessage(f);
		message.create(new FileInputStream("samples/queue2/msg4"));
		System.out.println(message.getHeader());
	}

	QueueMessage(File file) throws IOException {
		this(file, null);
	}

	QueueMessage(File file, MimeConfig mimeConfig) throws IOException {
		this.id = file.getName();
		this.file = file;
		this.mimeConfig = mimeConfig;

		// Load metadata
		File metaFile = getMetaFile();
		meta = metaFile.exists() ? MAPPER.readValue(getMetaFile(), Meta.class) : new Meta();
	}

	public void close() throws IOException {
		if (!deleted) {
			sync();
		}
		this.out = null;
		this.index = null;
		this.meta = null;
	}

	public long writeTo(OutputStream out) throws IOException {
		return this.writeTo(out, false);
	}

	public long writeTo(OutputStream out, boolean keepHeaderIntact) throws IOException {
		try {
			InputStream in;
			if (keepHeaderIntact) {
				// Use header and body together
				in = getInputStream();
			} else {
				// Use just body
				in = getBodyInputStream();
			}
			try {
				// Keep count of written bytes
				long written = 0;

				// Write modified header
				if (!keepHeaderIntact) {
					byte[] header = getHeader().toString().getBytes("iso-8859-1");
					out.write(header);
					written += header.length;
				}

				// Copy message body (or header+body)
				written += IOUtils.copyLarge(in, out);
				return written;
			} finally {
				in.close();
			}
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * Loads message body from input stream, copies it into message file,
	 * decodes textual body parts into multiple text files and save headers in a
	 * header file.
	 *
	 * @param in
	 * @throws IOException
	 * @throws MimeException
	 */
	public void create(InputStream in) throws IOException, MimeException {
		// this.meta = new Meta();
		// this.meta.setFrom(from);
		// this.meta.setRecipients(recipients);
		this.out = new DeferredFileOutputStream(128 * 1024, file);
		try {
			this.size = IOUtils.copy(in, out);
		} finally {
			this.out.close();
			in.close();
		}
		loadIndex(getInputStream());
	}

	public void delete() throws IOException {
		if (!deleted) {
			FileUtils.deleteQuietly(file);
			FileUtils.deleteQuietly(getMetaFile());
			this.out = null;
			this.index = null;
			this.meta = null;
			this.deleted = true;
		}
	}

	public void deliverTo(String recipient, String mailbox) {
		meta.deliverTo(recipient, mailbox);
	}

	public InputStream getBodyInputStream() throws IOException {
		InputStream in = getInputStream();
		// Search for empty line
		boolean newLineSeen = false;
		out: for (;;) {
			int c = in.read();
			switch (c) {
			case -1:
				break out;
			case '\n':
				if (newLineSeen) {
					break out;
				}
				newLineSeen = true;
			case '\r':
				break;
			default:
				newLineSeen = false;
			}
		}
		return in;
	}

	public List<Delivery> getDeliveries() {
		return meta.getDeliveries();
	}

	public Set<String> getFlags() {
		return meta.getFlags();
	}

	public String getFrom() {
		return meta.getFrom();
	}

	public Header getHeader() {
		return meta.getHeader();
	}

	public String getId() {
		return id;
	}

	public MemoryIndex getIndex() throws IOException, MimeException {
		if (index == null) {
			loadIndex(getInputStream());
		}
		return index;
	}

	public InputStream getInputStream() throws IOException {
		InputStream result;
		if (out != null) {
			result = out.toBufferedInputStream();
		} else {
			result = new BufferedInputStream(new FileInputStream(file));
		}
		return result;
	}

	public List<String> getRecipients() {
		return meta.getRecipients();
	}

	public int getSize() {
		return size != -1 ? size : (int) file.length();
	}

	/**
	 * Checks to see if all deliveries are completed
	 */
	public boolean isCompleted() {
		boolean completed = true;
		for (Delivery d : getDeliveries()) {
			if (!d.isCompleted()) {
				completed = false;
				break;
			}
		}
		return completed;
	}

	public void setCompleted() {
		setCompleted(null);
	}

	public void setCompleted(String status) {
		for (Delivery d : getDeliveries()) {
			if (!d.isCompleted()) {
				if (status != null) {
					d.setStatus(status);
				}
				d.setCompleted();
			}
		}
	}

	public void setDeliveries(List<Delivery> deliveries) {
		meta.setDeliveries(deliveries);
	}

	public void setFlags(Set<String> flags) {
		meta.setFlags(flags);
	}

	public void setFrom(String from) {
		meta.setFrom(from);
	}

	public void setHeader(Header header) {
		meta.setHeader(header);
	}

	public void setRecipients(List<String> recipients) {
		meta.setRecipients(recipients);
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("id", getId());
		b.append("meta", meta);
		return b.toString();
	}

	private File getMetaFile() {
		return new File(file.getParentFile(), id + ".json");
	}

	private void loadIndex(InputStream in) throws IOException, MimeException {
		MimeConfig mc = mimeConfig != null ? mimeConfig.clone() : new MimeConfig();

		this.index = new MemoryIndex();
		final Analyzer analyzer = new StandardAnalyzer();
		final Header allHeaders = new Header();
		final Header headers = new Header();
		final MimeStreamParser parser = new MimeStreamParser(mc);
		parser.setContentDecoding(true);
		parser.setContentHandler(new AbstractContentHandler() {

			@Override
			public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {
				String charset = bd.getCharset();
				if (charset != null) {
					String fieldName = "text-" + (++content);
					TokenStream stream = analyzer.tokenStream(fieldName, new BufferedReader(
							new InputStreamReader(is)));
					index.addField(fieldName, stream);
				}
			}

			@Override
			public void endBodyPart() throws MimeException {
				--depth;
			}

			@Override
			public void field(Field field) throws MimeException {
				String name = field.getName();
				String value = field.getBody();
				/*
				 * DecoderUtil.decodeEncodedWords(field.getBody(),
				 * DecodeMonitor.SILENT);
				 */
				if (depth == 0) {
					headers.add(name, value);
				}
				allHeaders.add(name, value);
			}

			@Override
			public void startBodyPart() throws MimeException {
				++depth;
			}

			@Override
			public void startMessage() throws MimeException {
			}

			private int depth;
			private int content;
		});
		try {
			parser.parse(in);
		} finally {
			IOUtils.closeQuietly(in);
		}

		// Add all header values
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, List<HeaderField>> e : allHeaders.getFields().entrySet()) {
			b.setLength(0);
			for (HeaderField f : e.getValue()) {
				b.append(f.getValue()).append(' ');
			}
			index.addField(e.getKey(), b.toString(), analyzer);
		}

		meta.setHeader(headers);
	}

	private void sync() throws IOException {
		if (meta.dirty) {
			MAPPER.writeValue(getMetaFile(), meta);
			meta.dirty = false;
		}

		if (out != null && out.isInMemory()) {
			out.writeToFile();
		}
	}

	private transient MimeConfig mimeConfig;

	private boolean deleted;

	private transient DeferredFileOutputStream out;

	private final File file;

	private final String id;

	private int size = -1;

	private Meta meta;

	private transient MemoryIndex index;
}