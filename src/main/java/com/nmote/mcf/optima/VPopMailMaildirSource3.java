package com.nmote.mcf.optima;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import com.nmote.maildir.Maildir;

public class VPopMailMaildirSource3 implements MaildirSource {

	@Inject
	public VPopMailMaildirSource3(@Named("domainsDir") String domainsDir) {
		this.domainsDir = new File(domainsDir);
		if (!this.domainsDir.isDirectory()) {
			throw new IllegalArgumentException("invalid domainsDir: " + domainsDir);
		}
	}

	public Maildir get(final String user, final String domain) throws MaildirNotFoundException {
		final String maildir = user + "/Maildir";
		final String base = domain + '/';
		File dir = new File(domainsDir, base + maildir);

		// Try 0..9
		for (int i = 0; !dir.exists() && i <= 9; ++i) {
			dir = new File(domainsDir, base + i + "/" + maildir);
		}

		// Try A..Z
		for (char i = 'A'; !dir.exists() && i <= 'Z'; ++i) {
			dir = new File(domainsDir, base + i + "/" + maildir);
		}

		// Try a..z
		for (char i = 'a'; !dir.exists() && i <= 'z'; ++i) {
			dir = new File(domainsDir, base + i + "/" + maildir);
		}

		// Second level
		// Try 0..9
		for (int j = 0; !dir.exists() && j <= 9; ++j) {
			// Try 0..9
			for (int i = 0; !dir.exists() && i <= 9; ++i) {
				dir = new File(domainsDir, base + j + "/" + i + "/" + maildir);
			}

			// Try A..Z
			for (char i = 'A'; !dir.exists() && i <= 'Z'; ++i) {
				dir = new File(domainsDir, base + j + "/" + i + "/" + maildir);
			}

			// Try a..z
			for (char i = 'a'; !dir.exists() && i <= 'z'; ++i) {
				dir = new File(domainsDir, base + j + "/" + i + "/" + maildir);
			}
		}

		dir = dir.getAbsoluteFile();
		if (!dir.exists()) {
			throw new MaildirNotFoundException(user + '@' + domain);
		}
		return new Maildir(dir);
	}

	private final File domainsDir;
}