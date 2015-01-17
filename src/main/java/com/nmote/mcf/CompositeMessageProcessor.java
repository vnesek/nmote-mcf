package com.nmote.mcf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CompositeMessageProcessor implements MessageProcessor {

	public CompositeMessageProcessor() {
		this(new ArrayList<MessageProcessor>());
	}
	
	public CompositeMessageProcessor(Collection<MessageProcessor> processors) {
		this.processors = processors;
	}
	
	public CompositeMessageProcessor(MessageProcessor... processors) {
		this(Arrays.asList(processors));
	}
	
	@Override
	public void check(QueueMessage message) throws IOException {
		for (MessageProcessor p : processors) {
			p.check(message);
		}
	}

	@Override
	public void checkRecipient(String recipient) throws RejectException {
		for (MessageProcessor p : processors) {
			p.checkRecipient(recipient);
		}
	}

	@Override
	public void deliver(QueueMessage message) throws IOException {
		for (MessageProcessor p : processors) {
			p.deliver(message);
		}
	}

	public Collection<MessageProcessor> getProcessors() {
		return processors;
	}

	@Override
	public void redeliver(QueueMessage message) throws IOException {
		for (MessageProcessor p : processors) {
			p.redeliver(message);
		}
	}

	@Override
	public void route(QueueMessage message) throws IOException {
		for (MessageProcessor p : processors) {
			p.route(message);
		}
	}

	private Collection<MessageProcessor> processors;
}
