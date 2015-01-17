package com.nmote.mcf.sieve;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.jsieve.ConfigurationManager;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.parser.generated.Node;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;

public class Module extends AbstractModule {

	@Override
	protected void configure() {
		bind(Node.class).toProvider(SieveProvider.class).in(Singleton.class);
		bind(LuceneQueryParser.class).toInstance(new CachingLuceneQueryParser(new DefaultLuceneQueryParser()));
		bind(SieveFactory.class).toProvider(new Provider<SieveFactory>() {
			public SieveFactory get() {
				return cm.build();
			}

			@Inject
			ConfigurationManager cm;
		}).in(Singleton.class);
	}

}
