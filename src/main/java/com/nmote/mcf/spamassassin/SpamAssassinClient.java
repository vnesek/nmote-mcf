package com.nmote.mcf.spamassassin;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Singleton
public class SpamAssassinClient {

    private static final String ENCODING = "us-ascii";

    public static void main(String[] args) throws IOException {
        SpamAssassinClient c = new SpamAssassinClient();
        File test = new File("samples/spamassassin-test.txt");
        System.err.println(c.check(new FileInputStream(test), test.length()));
    }

    /**
     * Tests input stream for spam.
     *
     * @param in
     * @param contentLength
     * @return
     * @throws IOException
     */
    public SpamAssassinResult check(InputStream message, long contentLength) throws IOException {
        Socket socket = new Socket(host, port);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        try {
            int total = 0;
            StringBuffer b = new StringBuffer(128);
            b.append("SYMBOLS SPAMC/").append(protocol).append("\r\n");
            b.append("Content-length: ").append(contentLength).append("\r\n");
            b.append("User: ").append(user).append("\r\n\r\n");
            String request = b.toString();
            out.write(request.getBytes(ENCODING));
            // log.debug("<< {}", request);
            byte[] buffer = new byte[bufferSize];
            for (; ; ) {
                int len = message.read(buffer);
                if (len == -1) {
                    break;
                } else {
                    out.write(buffer, 0, len);
                    total += len;
                }
            }
            // out.write("\r\n".getBytes(ENCODING));
            out.flush();
            log.debug("<< Checking {} bytes", total);
            SpamAssassinResult sar = new SpamAssassinResult();
            for (String line : IOUtils.readLines(in, StandardCharsets.ISO_8859_1)) {
                // log.debug(">> {}", line);
                if (line.startsWith("Spam: ")) {
                    String[] a = StringUtils.split(line, ":;/");
                    sar.setSpam(Boolean.parseBoolean(StringUtils.trim(a[1])));
                    sar.setScore(Float.parseFloat(StringUtils.trim(a[2])));
                    sar.setThreshold(Float.parseFloat(StringUtils.trim(a[3])));
                } else if (!line.startsWith("SPAMD/")) {
                    line = StringUtils.trimToNull(line);
                    if (line != null) {
                        sar.getSymbols().add(line);
                    }
                }
            }
            log.debug("{} ({} / {})", sar.isSpam() ? "Spam" : "Ok", sar.getScore(), sar.getThreshold());
            return sar;
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(socket);
        }
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        if (bufferSize < 128) {
            bufferSize = 128;
        }
        this.bufferSize = bufferSize;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (host == null) {
            throw new NullPointerException("host == null");
        }
        this.host = host;
    }

    public String getHostPort() {
        return getHost() + ':' + getPort();
    }

    @Inject
    public void setHostPort(@Named("spamassassin") String hostPort) {
        if (hostPort != null) {
            hostPort = hostPort.trim();
            int colon = hostPort.indexOf(':');
            if (colon >= 0) {
                setPort(Integer.parseInt(hostPort.substring(colon + 1)));
            }
            if (colon > 0) {
                setHost(hostPort.substring(0, colon));
            } else if (colon < 0) {
                setHost(hostPort);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("port <= 0: " + port);
        }
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    private int bufferSize = 1400;
    private String host = "localhost";
    private Logger log = LoggerFactory.getLogger(getClass());
    private int port = 783;
    private String protocol = "1.2";
    private String user = "mcf";

}
