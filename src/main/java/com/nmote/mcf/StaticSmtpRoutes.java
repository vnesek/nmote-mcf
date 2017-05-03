package com.nmote.mcf;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StaticSmtpRoutes implements Function<String, String> {

    @Inject
    public StaticSmtpRoutes(@Named("smtpRoutes") String spec) {
        this.routes = new HashMap<>();
        String defaultRoute = null;
        for (String s : StringUtils.split(spec, ' ')) {
            String[] r = StringUtils.splitByWholeSeparator(s, "=>");
            if (r.length != 2) {
                throw new IllegalArgumentException("invalid SMTP route spec: " + s);
            }
            if ("*".equals(r[0])) {
                defaultRoute = r[1];
            } else {
                routes.put(r[0], r[1]);
            }
        }
        this.defaultRoute = defaultRoute;
    }

    @Override
    public String apply(String domain) {
        return routes.getOrDefault(domain.toLowerCase(), defaultRoute);
    }

    private final Map<String, String> routes;
    private final String defaultRoute;
}
