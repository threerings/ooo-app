//
// ooo-app - a simple framework for (Java-based) social webapps
// Copyright (c) 2012, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/ooo-app/blob/master/etc/LICENSE

package com.threerings.app.client;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.app.data.AppCodes;

/**
 * Authentication utilities for app GWT clients.
 */
public class AuthUtil
{
    /**
     * Returns our session cookie if we have one, null if not.
     */
    public static String getAuthToken ()
    {
        String auth = CookieUtil.get(AppCodes.AUTH_COOKIE);
        return (auth == null || auth.length() == 0) ? null : auth;
    }

    /**
     * Clears out our session cookie if we have one.
     */
    public static void clearAuthToken ()
    {
        CookieUtil.clear("/", AppCodes.AUTH_COOKIE);
    }
}
