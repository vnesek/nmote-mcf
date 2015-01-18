package com.nmote.mcf.sieve;

import java.util.HashMap;
import java.util.Map;

import org.apache.jsieve.exception.SieveException;
import org.apache.lucene.search.Query;

public class CachingLuceneQueryParser implements LuceneQueryParser {

	public CachingLuceneQueryParser(LuceneQueryParser delegate) {
		assert delegate != null;

		this.delegate = delegate;
		this.cache = new HashMap<String, Query>();
	}

	public Query parse(String text) throws SieveException {
		Query result;
		synchronized (cache) {
			result = cache.get(text);
		}
		if (result == null) {
			result = delegate.parse(text);
			synchronized (cache) {
				cache.put(text, result);
			}
		}
		return result;
	}

	private LuceneQueryParser delegate;
	private Map<String, Query> cache;
}
