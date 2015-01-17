package com.nmote.counters;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultCounters implements Counters {

	static class Node {
		Node next;
		long time;
		long value;
	}

	public static void main(String[] args) {
		Counters c = new DefaultCounters();
		for (int x = 0; x < 10; ++x) {
			for (int y = 0; y < 100000; ++y) {
				c.add("cnt" + x, x * y);
			}
		}
		for (String n : c.counters()) {
			System.out.println(c + " " + c.collapse(n, System.currentTimeMillis()));
		}
	}

	@Override
	public void add(String counter, long value) {
		Node head = nodes.get(counter);
		if (head == null) {
			head = resetIntenal(counter);
		}

		long now = System.currentTimeMillis();
		synchronized (head) {
			Node node = new Node();
			node.value = value;
			node.time = now;
			node.next = head.next;
			head.next = node;
		}
	}

	/**
	 * Merge all counters older than time
	 * 
	 * @param counter
	 * @param time
	 * @return total counter value
	 */
	@Override
	public long collapse(String counter, long time) {
		long total = 0L;
		Node head = nodes.get(counter);
		if (head != null) {
			Node n = head.next;
			while (n != null && n.time >= time) {
				total += n.value;
				n = n.next;
			}
			if (n != null) {
				long mTotal = 0L;
				for (Node m = n.next; m != null; m = m.next) {
					mTotal += m.value;
				}
				n.next = null;
				n.value = n.value + mTotal;
				total += n.value;
			}
		}
		return total;
	}

	@Override
	public Collection<String> counters() {
		synchronized (nodes) {
			return new TreeSet<String>(nodes.keySet());
		}
	}

	@Override
	public void reset(String counter) {
		resetIntenal(counter);
	}

	@Override
	public String toString() {
		Collection<String> counters = counters();
		StringBuilder b = new StringBuilder();
		b.append('{');
		for (String c : counters) {
			b.append(c);
			b.append(':');
			b.append(value(c));
			b.append(", ");
		}
		int len = b.length();
		b.setCharAt(b.length() - 2, '}');
		b.setLength(len - 1);
		return b.toString();
	}

	@Override
	public long value(String counter) {
		return value(counter, 0L);
	}

	@Override
	public long value(String counter, long time) {
		long total = 0L;
		Node head = nodes.get(counter);
		if (head != null) {
			for (Node n = head.next; n != null && n.time >= time; n = n.next) {
				total += n.value;
			}
		}
		return total;
	}

	private Node resetIntenal(String counter) {
		synchronized (nodes) {
			Node head = nodes.get(counter);
			if (head == null) {
				head = new Node();
				head.next = new Node();
				nodes.put(counter, head);
			}
			return head;
		}
	}

	private ConcurrentMap<String, Node> nodes = new ConcurrentHashMap<String, Node>();
}
