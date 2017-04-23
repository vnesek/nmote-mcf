package com.nmote.mcf.optima;

public class MaildirNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MaildirNotFoundException(String message) {
        super(message);
    }
}
