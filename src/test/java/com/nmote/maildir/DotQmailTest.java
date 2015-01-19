package com.nmote.maildir;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

public class DotQmailTest {

	@Test
	public void dotQmail1() throws IOException {
		DotQmail dq1 = new DotQmail().load(getClass().getResourceAsStream("dot-qmail-1.txt"));

		assertEquals(Arrays.asList("# Some comment"), dq1.commentLines());
		assertEquals(Arrays.asList("/home/vpopmail/domains/zzada.hr/q/ranka.duggu/Maildir/"), dq1.maildirLines());
		assertEquals(Arrays.asList("rduggu@gmail.com"), dq1.forwardLines());
		assertTrue(dq1.mailboxLines().isEmpty());
	}

}
