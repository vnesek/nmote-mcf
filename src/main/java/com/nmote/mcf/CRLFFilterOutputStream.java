package com.nmote.mcf;

import org.apache.commons.lang3.StringUtils;

import java.io.*;

/**
 * Replaces CRLFs with LFs
 *
 * @author vnesek
 */
public class CRLFFilterOutputStream extends FilterOutputStream {

    private static final int CR = '\r';
    private static final int LF = '\n';

    public CRLFFilterOutputStream(OutputStream out) {
        super(out);
    }

    public static void main(String... args) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(new CRLFFilterOutputStream(baos), "utf-8");
        w.write("From china with love\n");
        w.write("\r\r\r From Hello world\r\n");
        w.write(">>>>From  4x china with love\n\r");
        w.write(">>>>>>>>>>From 10x china with love\n");
        w.write(">>>From china  3x with love\r\n\r\n");
        w.close();

        String s = baos.toString("utf-8");
        s = StringUtils.replaceEach(s, new String[]{"\r", "\n"}, new String[]{"[CR]", "[LF]\n"});
        System.out.println(s);
    }

    @Override
    public void close() throws IOException {
        if (cr) {
            super.write(CR);
            cr = false;
        }
        super.close();
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        for (int i = 0; i < len; ++i) {
            writeInternal((int) bytes[off + i]);
        }
    }

    @Override
    public void write(int b) throws IOException {
        writeInternal(b);
    }

    private final void writeInternal(int b) throws IOException {
        if (cr) {
            if (b == LF) {
                super.write(LF);
                cr = false;
            } else if (b == CR) {
                super.write(CR);
            } else {
                super.write(CR);
                super.write(b);
                cr = false;
            }
        } else {
            if (b == CR) {
                cr = true;
            } else {
                super.write(b);
            }
        }
    }

    private boolean cr;
}
