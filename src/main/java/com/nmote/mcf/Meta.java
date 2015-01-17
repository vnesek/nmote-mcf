package com.nmote.mcf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

class Meta {

	public List<Delivery> getDeliveries() {
		return deliveries;
	}

	public String getFrom() {
		return from;
	}

	@JsonProperty("header")
	public Header getHeader() {
		return header;
	}

	public List<String> getRecipients() {
		if (recipients == null) {
			recipients = new ArrayList<String>();
		}
		return recipients;
	}

	public void setDeliveries(List<Delivery> deliveries) {
		this.deliveries = deliveries;
		this.dirty = true;
	}

	public void setFrom(String from) {
		this.from = from;
		this.dirty = true;
	}

	public void setHeader(Header headers) {
		this.header = headers;
		this.dirty = true;
	}

	public void setRecipients(List<String> recipients) {
		this.recipients = recipients;
		this.dirty = true;
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("from", from);
		b.append("rcpt", recipients);
		b.append("header", header);
		return b.toString();
	}

	public void deliverTo(String recipient, String mailbox) {
		for (Delivery d : deliveries) {
			if (d.getDestination().equals(mailbox)) {
				if (d.addRecipient(recipient)) {
					dirty = true;
				}
				return;
			}
		}
		Delivery d = new Delivery();
		d.setDestination(mailbox);
		d.addRecipient(recipient);
		deliveries.add(d);
		dirty = true;
	}

	public Set<String> getFlags() {
		if (flags == null) {
			flags = new HashSet<String>();
		}
		return flags;
	}

	public void setFlags(Set<String> flags) {
		this.flags = flags;
	}

	private Set<String> flags;

	private String from;

	private List<String> recipients;

	private Header header = new Header();

	private List<Delivery> deliveries = new ArrayList<Delivery>();

	@JsonIgnore
	volatile boolean dirty;
}