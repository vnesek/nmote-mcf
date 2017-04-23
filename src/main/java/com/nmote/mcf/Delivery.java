package com.nmote.mcf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Delivery {

    public boolean addRecipient(String recipient) {
        return getRecipients().add(recipient);
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Set<String> getRecipients() {
        if (recipients == null) {
            recipients = new HashSet<String>();
        }
        return recipients;
    }

    public void setRecipients(Set<String> recipients) {
        this.recipients = recipients;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public boolean isCompleted() {
        return complete != null;
    }

    public void setCompleted() {
        setComplete(new Date());
    }

    public Date getComplete() {
        return complete;
    }

    public void setComplete(Date complete) {
        this.complete = complete;
    }

    @Override
    public String toString() {
        ToStringBuilder b = new ToStringBuilder(this);
        b.append("dest", destination);
        b.append("status", status);
        b.append("rcpt", recipients);
        return b.toString();
    }

    private Set<String> recipients;
    private String status;
    private Date complete;
    private String destination;
}
