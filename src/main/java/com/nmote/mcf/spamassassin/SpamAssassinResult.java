package com.nmote.mcf.spamassassin;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SpamAssassinResult implements Serializable {

	private static final long serialVersionUID = 7627002624553177160L;

	public float getScore() {
		return score;
	}

	public Collection<String> getSymbols() {
		if (symbols == null) {
			symbols = new HashSet<String>();
		}
		return symbols;
	}

	public float getThreshold() {
		return threshold;
	}

	public boolean isSpam() {
		return spam;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public void setSpam(boolean spam) {
		this.spam = spam;
	}

	public void setSymbols(Collection<String> symbols) {
		this.symbols = symbols;
	}

	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("spam", spam);
		b.append("score", score);
		b.append("threshold", threshold);
		b.append("symbols", symbols);
		return b.toString();
	}

	private boolean spam;
	private float score;
	private float threshold;
	private Collection<String> symbols;
}
