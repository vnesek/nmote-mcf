package com.nmote.maildir;

import java.io.Serializable;

public class Quota implements Serializable {

    private static final long serialVersionUID = -4697979652948963884L;

    public int getMaxMessageCount() {
        return maxMessageCount;
    }

    public void setMaxMessageCount(int maxMessages) {
        this.maxMessageCount = maxMessages;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messages) {
        this.messageCount = messages;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long used) {
        this.size = used;
    }

    public boolean isOverQuota() {
        return (maxSize != -1 && size > maxSize) || (maxMessageCount != -1 && messageCount > maxMessageCount);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(40);
        b.append(size).append('/').append(maxSize).append("S,");
        b.append(messageCount).append('/').append(maxMessageCount).append('C');
        return b.toString();
    }

    private int maxMessageCount = -1;
    private long maxSize = -1;
    private long size;
    private int messageCount;
}
