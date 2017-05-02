package com.nmote.maildir;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DotQmailTest {

    @Test
    public void dotQmail1() throws IOException {
        DotQmail dq1 = new DotQmail().load(getClass().getResourceAsStream("dot-qmail-1.txt"));

        assertEquals(Collections.singletonList("# Some comment"), dq1.commentLines());
        assertEquals(Collections.singletonList("/home/vpopmail/domains/zzada.hr/q/ranka.duggu/Maildir/"), dq1.maildirLines());
        assertEquals(Collections.singletonList("rduggu@gmail.com"), dq1.forwardLines());
        assertTrue(dq1.mailboxLines().isEmpty());
    }

}
