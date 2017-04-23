package com.nmote.maildir;

import java.io.File;

class UnixUtils {

    public static void chmod(int mod, File file) {
        if ((mod & 0444) != 0) {
            file.setReadable(true, (mod & 0044) == 0);
        }
        if ((mod & 0222) != 0) {
            file.setWritable(true, (mod & 0022) == 0);
        }
        if ((mod & 0111) != 0) {
            file.setExecutable(true, (mod & 0011) == 0);
        }
    }

    public static void touch(File file) {
        file.setLastModified(System.currentTimeMillis());
    }

}
