package com.nmote.mcf.sieve.commands;

import org.apache.jsieve.mail.ActionFileInto;

public class ActionRoute extends ActionFileInto {

    public ActionRoute(String destination) {
        super(destination.startsWith("smtp:") ? destination : ("smtp:" + destination));
    }
}
