package com.nmote.maildir;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Folder {

    Folder(Maildir maildir, Folder parent, File dir) {
        this.maildir = maildir;
        this.parent = parent;
        this.dir = dir;
    }

    public Folder createSubFolder(String folder) throws IOException {
        String name = "." + folder;

        // UnixUtils.chmod(700, createSubdir(name));
        // UnixUtils.chmod(700, createSubdir(name + "/tmp"));
        // UnixUtils.chmod(700, createSubdir(name + "/new"));
        // UnixUtils.chmod(700, createSubdir(name + "/cur"));
        // UnixUtils.touch(new File(dir, name + "/maildir"));
        FileUtils.forceMkdir(new File(dir, name + "/tmp"));
        FileUtils.forceMkdir(new File(dir, name + "/new"));
        FileUtils.forceMkdir(new File(dir, name + "/cur"));
        return new Folder(maildir, this, new File(dir, name));
    }

    public void clean() throws IOException {
        try {
            FileUtils.cleanDirectory(new File(dir, "tmp"));
            FileUtils.cleanDirectory(new File(dir, "new"));
            FileUtils.cleanDirectory(new File(dir, "cur"));
        } finally {
            maildir.recalculateMaildirSize();
        }
    }

    public File getDir() {
        return dir;
    }

    public Maildir getMaildir() {
        return maildir;
    }

    public String getName() {
        String name = dir.getName();
        int dot = name.lastIndexOf('.');
        assert dot != -1;
        return name.substring(dot + 1);
    }

    public Folder getParent() {
        return parent;
    }

    public List<Folder> list() {
        final String prefix = (parent != null ? dir.getName() : "") + ".";
        File[] files = maildir.getDir().listFiles(new FileFilter() {
            public boolean accept(File file) {
                String name = file.getName();
                return file.isDirectory() && name.startsWith(prefix) && name.indexOf('.', prefix.length() + 1) == -1;
            }

            ;
        });
        if (files != null) {
            List<Folder> result = new ArrayList<Folder>(files.length);
            for (File file : files) {
                result.add(new Folder(maildir, this, file));
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    public Folder getFolder(String name) {
        for (Folder f : list()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    private final File dir;
    private final Maildir maildir;
    private final Folder parent;
}
