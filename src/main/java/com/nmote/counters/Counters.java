package com.nmote.counters;

import java.util.Collection;

public interface Counters {

	void add(String counter, long value);
	
	long collapse(String counter, long time);

	Collection<String> counters();

	void reset(String counter);
	
	long value(String counter);

	long value(String counter, long time);
}
