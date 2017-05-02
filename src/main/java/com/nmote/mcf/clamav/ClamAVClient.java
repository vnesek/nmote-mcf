package com.nmote.mcf.clamav;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.net.Socket;

/**
 * ClamAV client, implemented according to protocol specification at:
 * http://www.clamav.net/doc/latest/html/node28.html
 *
 * @author vnesek
 */
public class ClamAVClient implements Closeable {

    private static final String ENCODING = "us-ascii";

    public enum ScanMode {
        /**
         * Scan file or directory (recursively) with archive support enabled and
         * don't stop the scanning when a virus is found.
         */
        CONTSCAN,

        /**
         * Scan file in a standard way or scan directory (recursively) using
         * multiple threads (to make the scanning faster on SMP machines).
         */
        MULTISCAN,

        /**
         * Scan file or directory (recursively) with archive and special file
         * support disabled
         */
        RAWSCAN,

        /**
         * Scan file or directory (recursively) with archive support enabled (a
         * full path is required).
         */
        SCAN
    }

    public static void main(String[] args) throws IOException {
        ClamAVClient c = new ClamAVClient();
        // c.stats();
        c.version();
        // System.out.println(c.ping());
        // System.err.println(c.instream(new
        // ByteArrayInputStream(test.getBytes())));
        System.err.println(c.instream(new FileInputStream("samples/eicar.txt")));
        c.close();
    }

    private static void toNetworkOrder(int c, byte[] b) {
        b[0] = (byte) ((c >> 24) & 0xff);
        b[1] = (byte) ((c >> 16) & 0xff);
        b[2] = (byte) ((c >> 8) & 0xff);
        b[3] = (byte) (c & 0xff);
    }

    @Override
    public synchronized void close() throws IOException {
        if (isConnected()) {
            try {
                send("END");
            } catch (IOException ignored) {
            }
            try {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
                socket.close();
                log.debug("(disconnected)");
            } finally {
                in = null;
                out = null;
                socket = null;
            }
        }
    }

    public synchronized void connect() throws IOException {
        if (!isConnected()) {
            socket = new Socket(host, port);
            in = new BufferedInputStream(socket.getInputStream(), 256);
            out = socket.getOutputStream();
            log = LoggerFactory.getLogger("clamav." + host.replace('.', '_') + "_" + port);
            log.debug("(connected)");
            out.write("zIDSESSION\0".getBytes(ENCODING));
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
    public void setHostPort(@Named("clamAV") String hostPort) {
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

    public int getMaxInstreamLength() {
        return maxInstreamLength;
    }

    public void setMaxInstreamLength(int maxInstreamLength) {
        if (maxInstreamLength < bufferSize * 4) {
            throw new IllegalArgumentException("instreamLength < bufferSize * 4: " + maxInstreamLength);
        }
        this.maxInstreamLength = maxInstreamLength;
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

    /**
     * Tests input stream for viruses. If stream in clean returns null otherwise
     * returns virus signature found. Input stream isn't closed.
     *
     * @param in
     * @return
     * @throws IOException
     */
    public synchronized String instream(InputStream in) throws IOException {
        connect();
        int total = 0;
        send("INSTREAM");
        byte[] length = new byte[4];
        byte[] buffer = new byte[bufferSize];
        for (; ; ) {
            int len = in.read(buffer);
            if (len == -1) {
                toNetworkOrder(0, length);
                out.write(length);
                out.flush();
                log.debug("<< (instream done)");
                break;
            } else {
                toNetworkOrder(len, length);
                out.write(length);
                out.write(buffer, 0, len);
                total += len;
                log.debug("<< (instream {} bytes)", total);
            }

            if (bufferSize + total > maxInstreamLength) {
                toNetworkOrder(0, length);
                out.write(length);
                out.flush();
                log.debug("<< (instream too long, done)");
                break;
            }
        }
        String result = receive();
        if (result.startsWith("stream: ")) {
            result = StringUtils.trimToNull(result.substring(8));
        }
        if ("OK".equalsIgnoreCase(result)) {
            result = null;
        }
        return result;
    }

    /**
     * Returns true if client is connected
     *
     * @return
     */
    public boolean isConnected() {
        return socket != null;
    }

    /**
     * Check the daemon's state (should reply with "PONG").
     *
     * @return true if reponse is "PONG"
     * @throws IOException
     */
    public synchronized boolean ping() throws IOException {
        return "PONG".equals(sendAndReceive("PING"));
    }

    /**
     * Scan file or directory (recursively) with archive support enabled (a full
     * path is required).
     *
     * @param path
     * @return
     * @throws IOException
     */
    public synchronized String scan(String path) throws IOException {
        return scan(path, ScanMode.SCAN);
    }

    /**
     * Scan file or directory according to scan mode
     *
     * @param path
     * @param mode
     * @return
     * @throws IOException
     */
    public synchronized String scan(String path, ScanMode mode) throws IOException {
        String normalizedPath = FilenameUtils.normalize(path);
        return sendAndReceive(mode.toString() + ' ' + normalizedPath);
    }

    /**
     * On this command clamd provides statistics about the scan queue, contents
     * of scan queue, and memory usage. The exact reply format is subject to
     * changes in future releases.
     *
     * @return stats
     * @throws IOException
     */
    public synchronized String stats() throws IOException {
        return sendAndReceive("STATS");
    }

    /**
     * Print program and database versions.
     *
     * @return ClamAV version
     * @throws IOException
     */
    public synchronized String version() throws IOException {
        return sendAndReceive("VERSION");
    }

    private synchronized String receive() throws IOException {
        out.flush();
        int id = -1;
        StringBuilder buffer = new StringBuilder(256);
        for (int c = in.read(); ; c = in.read()) {
            if (c == 0) {
                break;
            } else if (c == -1) {
                close();
                break;
            } else {
                if (c == ':' && id == -1) {
                    id = Integer.parseInt(buffer.toString());
                    buffer.setLength(0);
                } else if (c == ' ' && buffer.length() == 0) {
                    // Skip
                } else {
                    buffer.append((char) c);
                }
            }
        }
        String response = buffer.toString();
        log.debug(">> {}", response);
        return response;
    }

    private synchronized void send(String request) throws IOException {
        log.debug("<< {}", request);
        out.write((byte) 'z');
        out.write(request.getBytes(ENCODING));
        out.write(0);
    }

    private synchronized String sendAndReceive(String request) throws IOException {
        connect();
        send(request);
        return receive();
    }

    private int bufferSize = 1400;
    private String host = "localhost";
    private InputStream in;
    private Logger log;
    private int maxInstreamLength = 10 * 1024 * 1024;
    private OutputStream out;
    private int port = 3310;
    private Socket socket;
}
