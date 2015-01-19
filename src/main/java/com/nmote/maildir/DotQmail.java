package com.nmote.maildir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DotQmail implements Iterable<String> {

	public DotQmail() {
		lines = new ArrayList<>();
	}

	public DotQmail add(String line) {
		lines.add(line);
		return this;
	}

	public DotQmail clear() {
		lines.clear();
		return this;
	}

	public List<String> commentLines() {
		return prefixedLines("#");
	}

	public List<String> forwardLines() {
		List<String> result = new ArrayList<>(lines.size());
		for (String line : lines) {
			if (line.length() > 0) {
				char c = line.charAt(0);
				if (c == '&') {
					result.add(line.substring(1));
				} else if (c != '#' && c != '.' && c != '/' && c != '|') {
					result.add(line);
				}
			}
		}
		return result;
	}

	@Override
	public Iterator<String> iterator() {
		return lines.iterator();
	}

	public DotQmail load(InputStream in) throws IOException {
		clear();
		for (String line : IOUtils.readLines(in)) {
			lines.add(line);
		}
		return this;
	}

	public List<String> mailboxLines() {
		return mailLines(true);
	}

	public List<String> maildirLines() {
		return mailLines(false);
	}

	public List<String> programLines() {
		return prefixedLines("|");
	}

	public DotQmail remove(String line) {
		while (lines.remove(line)) {
		}
		return this;
	}

	public DotQmail save(OutputStream out) throws IOException {
		IOUtils.writeLines(lines, "\n", out);
		return this;
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("lines", lines);
		return b.toString();
	}

	private List<String> mailLines(boolean mailbox) {
		List<String> result = new ArrayList<>(lines.size());
		for (String line : lines) {
			if (line.length() > 1) {
				char c = line.charAt(0);
				if ((c == '/' || c == '.') && (mailbox ^ line.endsWith("/"))) {
					result.add(line);
				}
			}
		}
		return result;
	}

	private List<String> prefixedLines(String start) {
		List<String> result = new ArrayList<>(lines.size());
		for (String line : lines) {
			if (line.startsWith(start)) {
				result.add(line);
			}
		}
		return result;
	}

	private List<String> lines;
}
