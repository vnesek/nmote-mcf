package com.nmote.mcf.sieve;

import org.apache.jsieve.exception.SieveException;
import org.apache.lucene.search.Query;

public interface LuceneQueryParser {

	Query parse(String text) throws SieveException;
}
