package com.nmote.mcf.optima;

import com.nmote.xr.XRMethod;

import java.util.Map;

public interface Routing {

    /**
     * Resolves id, username, login or email alias to a primary email address of
     * a user
     *
     * @param id
     * @param domain Domain to search for user
     * @return resolved email address
     */
    @XRMethod("routing.resolveEmail")
    String resolveEmail(String id, String domain);

    /**
     * Resolves id, username, login or email alias to a primary email address of
     * a user and additional account parameters.
     *
     * @param id
     * @param domain Domain to search for user
     * @return resolved email address and additional parameters
     */
    @XRMethod("routing.resolveEmailEx")
    Map<String, String> resolveEmailEx(String id, String domain);
}
