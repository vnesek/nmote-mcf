package com.nmote.mcf.sieve;

import org.apache.jsieve.exception.SieveException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.Query;


public class DefaultLuceneQueryParser implements LuceneQueryParser {

    private static final String[] FIELDS = {"text-1", "text-2", "text-3", "text-4", "text-5", "text-6", "text-7",
            "text-8", "text-9", "text-10", "to", "cc", "from", "bcc", "delivered-to"};

    public synchronized Query parse(String text) throws SieveException {
        try {
            return parser.parse(text);
        } catch (Exception e) {
            throw new SieveException(e);
        }
    }

    private MultiFieldQueryParser parser = new MultiFieldQueryParser(FIELDS, new StandardAnalyzer(
    ));

}
