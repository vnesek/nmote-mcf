package com.nmote.counters;

import java.util.Collection;
import java.util.Collections;

public class NullCounters implements Counters {

	@Override
	public void add(String counter, long value) {
	}

	@Override
	public long collapse(String counter, long time) {
		return 0;
	}

	@Override
	public Collection<String> counters() {
		return Collections.emptySet();
	}

	@Override
	public void reset(String counter) {
	}

	@Override
	public long value(String counter) {
		return 0;
	}

	@Override
	public long value(String counter, long time) {
		return 0;
	}
}
