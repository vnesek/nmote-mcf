package com.nmote.maildir;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Maildir {

    public Maildir(File dir) {
        if (!"Maildir".equals(dir.getName())) {
            throw new IllegalArgumentException("expected 'Maildir' as a directory name: " + dir);
        }
        this.dir = dir;
    }

    public static void main(String[] args) throws IOException {
        Maildir m = new Maildir(new File("/tmp/x/Maildir"));
        // m.create(1000000, 20000);
        m.getRootFolder().getFolder("Thrash").clean();
    }

    private static void safeRenameTo(File source, File dest) throws IOException {
        if (!source.renameTo(dest)) {
            if (dest.exists()) {
                if (!dest.delete()) {
                    throw new IOException("failed to delete " + dest);
                }
                if (!source.renameTo(dest)) {
                    throw new IOException("failed to replace " + dest);
                }
            } else {
                throw new IOException("failed to replace " + dest);
            }
        }
    }

    public void create(Quota quota) throws IOException {
        if (quota.getMaxSize() < 1000 && quota.getMaxSize() != -1) {
            throw new IllegalArgumentException("maxSize < 1000: " + quota.getMaxSize());
        }
        if (quota.getMaxMessageCount() < 10 && quota.getMaxMessageCount() != -1) {
            throw new IllegalArgumentException("maxMessageCount < 10: " + quota.getMaxMessageCount());
        }

        this.quota = quota;

        FileUtils.forceMkdir(new File(dir, "tmp"));
        FileUtils.forceMkdir(new File(dir, "new"));
        FileUtils.forceMkdir(new File(dir, "cur"));

        recalculateMaildirSize();

        // Create thrash folder
        Folder root = getRootFolder();
        root.createSubFolder("drafts");
        root.createSubFolder("spam");
        root.createSubFolder("sent-mail");
        root.createSubFolder("trash");
    }

    public File getDir() {
        return dir;
    }

    public Quota getQuota(int maxMessageCount, long maxSize) {
        maildirSize(maxMessageCount, maxSize);
        return quota;
    }

    public Folder getRootFolder() {
        return new Folder(this, null, dir);
    }

    void recalculateMaildirSize() throws IOException {
        File[] files = dir.listFiles();
        long size = 0;
        int messageCount = 0;
        if (files != null) {
            // Iterate over all files
            for (File f : files) {
                if (f.isDirectory()) {
                    if (f.getName().startsWith(".")) {
                        // This is folder, iterate over subfolders contents only
                        File[] folders = f.listFiles();
                        if (folders != null) {
                            for (File s : folders) {
                                if (s.isDirectory()) {
                                    File[] zubs = s.listFiles();
                                    if (zubs != null) {
                                        for (File z : zubs) {
                                            if (z.isFile()) {
                                                ++messageCount;
                                                size += z.length();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Iterate over files in a subdir
                        File[] subs = f.listFiles();
                        if (subs != null) {
                            for (File s : subs) {
                                if (s.isFile()) {
                                    ++messageCount;
                                    size += s.length();
                                }
                            }
                        }
                    }
                }
            }
        }
        quota.setSize(size);
        quota.setMessageCount(messageCount);
        String mds = quota.getMaxSize() + "S," + quota.getMaxMessageCount() + "C\n";
        if (size != 0 || messageCount != 0) {
            mds += size + " " + messageCount + "\n";
        }
        FileUtils.write(new File(dir, "maildirsize"), mds, StandardCharsets.ISO_8859_1);
    }

    public void removeFile(String name) throws IOException {
        File file = getFile(name);
        if (file.exists() && !file.delete()) {
            throw new IOException("can't delete " + file);
        }
    }

    public void updateMaildirSize(int size, int count) throws IOException {
        FileUtils.write(new File(dir, "maildirsize"), size + " " + count, StandardCharsets.ISO_8859_1, true);
    }

    public void writeFile(String name, List<String> lines) throws IOException {
        File tmp = File.createTempFile("omp", null, new File(getDir(), "tmp"));
        BufferedWriter out = new BufferedWriter(new FileWriter(tmp));
        for (String line : lines) {
            if (line != null) {
                out.write(line);
                out.newLine();
            }
        }
        out.close();

        // Replace files
        safeRenameTo(tmp, getFile(name));
    }

    private File getFile(String name) {
        return new File(getDir(), name);
    }

    public boolean maildirSizeExists() {
        return new File(dir, "maildirsize").exists();
    }

    private void maildirSize(int maxMessageCount, long maxSize) {
        if (quota == null) {
            try {
                File maildirSize = new File(dir, "maildirsize");
                if (!maildirSize.exists()) {
                    quota = new Quota();
                    quota.setMaxMessageCount(maxMessageCount);
                    quota.setMaxSize(maxSize);
                    recalculateMaildirSize();
                    return;
                }

                quota = new Quota();
                try (BufferedReader in = new BufferedReader(new FileReader(maildirSize), 5120)) {
                    { // Parse quota definition
                        String line = in.readLine();
                        if (line != null) {
                            for (String q : StringUtils.split(line, ", \t")) {
                                int len = q.length();
                                char type = q.charAt(len - 1);
                                int value = Integer.parseInt(q.substring(0, len - 1));
                                switch (type) {
                                    case 'S':
                                        quota.setMaxSize(value);
                                        break;
                                    case 'C':
                                        quota.setMaxMessageCount(value);
                                        break;
                                }
                            }
                        }
                    }

                    { // Calculate current total message size and count
                        long size = 0;
                        int messageCount = 0;
                        for (String line = in.readLine(); line != null; line = in.readLine()) {
                            String[] a = StringUtils.split(line, " \t");
                            size += Long.parseLong(a[0]);
                            messageCount += Integer.parseInt(a[1]);
                        }

                        // Assign read size to fields
                        quota.setSize(size);
                        quota.setMessageCount(messageCount);
                    }
                }

                if (maildirSize.length() >= 5120) {
                    String mds = quota.getMaxSize() + "S," + quota.getMaxMessageCount() + "C\n";
                    FileUtils.write(maildirSize, mds, StandardCharsets.ISO_8859_1);
                }

            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "maildir:" + dir.getAbsolutePath();
    }

    private final File dir;

    private Quota quota;
}