package com.nmote.mcf;

import org.apache.commons.lang3.StringUtils;

import java.io.*;

public class MBoxOutputStream extends FilterOutputStream {

    private static final int CR = '\r';

    private static final int LF = '\n';

    private static final int[] FROM_ = {'F', 'r', 'o', 'm', ' '};

    public MBoxOutputStream(OutputStream out) {
        super(out);
    }

    public static void main(String... args) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(new MBoxOutputStream(baos), "utf-8");
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
        if (state == CR) {
            super.write(CR);
        } else if (state > 0 && state < LF) {
            flushBuffer();
        }
        super.close();
    }

    @Override
    public void flush() throws IOException {
        if (state == 0 || state == LF) {
            super.flush();
        }
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

    private void flushBuffer() throws IOException {
        for (int i = 0; i < state; ++i) {
            super.write(FROM_[i]);
        }
    }

    private void writeInternal(int b) throws IOException {
        switch (state) {
            case 0:
                // BOL
                if (b == '>') {
                    super.write(b);
                    break;
                }
            case 1:
            case 2:
            case 3:
            case 4:
                if (FROM_[state] == b) {
                    ++state;
                    if (state == 5) {
                        super.write('>');
                        flushBuffer();
                        state = LF;
                    }
                } else if (b == CR) {
                    state = CR;
                } else {
                    flushBuffer();
                    super.write(b);
                    state = LF;
                }
                break;
            case CR:
                if (b == LF) {
                    super.write(LF);
                    state = 0;
                } else if (b == CR) {
                    super.write(CR);
                } else {
                    super.write(CR);
                    super.write(b);
                    state = LF;
                }
                break;
            case LF:
                if (b == CR) {
                    state = CR;
                } else {
                    super.write(b);
                    if (b == LF) {
                        state = 0;
                    }
                }
                break;
            default:
                throw new AssertionError();
        }
    }

    private int state = 0;
}
