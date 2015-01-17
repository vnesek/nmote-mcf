package com.nmote.mcf.optima;

import com.nmote.maildir.Maildir;

public interface MaildirSource {

	Maildir get(String user, String domain) throws MaildirNotFoundException;
}
