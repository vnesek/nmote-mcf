package com.nmote.mcf.optima;

import org.apache.commons.lang3.StringUtils;

public class PathRename {

    public PathRename(String spec) {
        String[] s = StringUtils.split(spec, ' ');
        int m = s.length;
        this.searchList = new String[m];
        this.replacementList = new String[m];
        for (int i = 0; i < m; ++i) {
            String[] r = StringUtils.splitByWholeSeparator(s[i], "=>");
            if (r.length != 2) {
                throw new IllegalArgumentException("invalid pat rename spec: " + r);
            }
            this.searchList[i] = r[0];
            this.replacementList[i] = r[1];
        }
    }

    public String rename(String text) {
        return StringUtils.replaceEach(text, searchList, replacementList);
    }

    private final String[] replacementList;
    private final String[] searchList;
}
