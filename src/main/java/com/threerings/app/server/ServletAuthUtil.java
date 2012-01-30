//
// ooo-app - a simple framework for (Java-based) social webapps
// Copyright (c) 2012, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/ooo-app/blob/master/etc/LICENSE

package com.threerings.app.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.app.data.AppCodes;

/**
 * Provides auth-related utilities to servlets.
 */
public class ServletAuthUtil
{
    /**
     * Returns the auth cookie in the supplied request, or null.
     */
    public static String getAuthCookie (HttpServletRequest req)
    {
        return CookieUtil.getCookieValue(req, AppCodes.AUTH_COOKIE);
    }

    /**
     * Adds an auth cookie to the supplied servlet response
     */
    public static void addAuthCookie (HttpServletRequest req, HttpServletResponse rsp,
                                      String authtok, int sessionDays)
    {
        addCookie(req, rsp, AppCodes.AUTH_COOKIE, authtok, sessionDays);
    }

    /**
     * Returns the scheme to use when constructing a URL based on the supplied request. If the
     * request itself is https, https is returned, or if the request is http but the load-balancing
     * proxy header indicating that the original request was https is found, https is returned.
     * Otherwise http is returned.
     */
    public static String getScheme (HttpServletRequest req)
    {
        String scheme = req.getScheme();
        String proxyScheme = req.getHeader("X-Forwarded-Proto");
        return ("https".equals(proxyScheme) || "https".equals(scheme)) ? "https" : scheme;
    }

    /**
     * Creates a URL with scheme, server name, and port as appropriate for the supplied request
     * (accounting for proxied https) and using the supplied path plus query parameters.
     */
    public static String createURL (HttpServletRequest req, String pathEtc)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(getScheme(req)).append("://").append(req.getServerName());
        if (req.getServerPort() != 80) {
            buf.append(":").append(req.getServerPort());
        }
        buf.append(pathEtc.startsWith("/") ? "" : "/").append(pathEtc);
        return buf.toString();
    }

    protected static void addCookie (HttpServletRequest req, HttpServletResponse rsp,
                                     String key, Object value, int maxAge)
    {
        Cookie cookie = new Cookie(key, String.valueOf(value));
        cookie.setMaxAge(maxAge * 24*60*60);
        cookie.setPath("/");
        if (!req.getServerName().equals("localhost")) {
            // we'll be requested as something like 'apps.threerings.net' and we want this cookie
            // to be visible to billing.apps.threerings.net so we need to prefix domain with a .
            cookie.setDomain("." + req.getServerName());
        }
        rsp.addCookie(cookie);
    }
}
