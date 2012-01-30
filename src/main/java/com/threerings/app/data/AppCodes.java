//
// ooo-app - a simple framework for (Java-based) social webapps
// Copyright (c) 2012, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/ooo-app/blob/master/etc/LICENSE

package com.threerings.app.data;

/**
 * Codes and constants relating to apps.
 */
public final class AppCodes
{
    /** The name of the cookie that contains our auth token. */
    public static final String AUTH_COOKIE = "auth";

    /** The name of the cookie that contains parameter information. */
    public static final String PARAMETER_COOKIE = "params";

    /** The name of the timestamp value within the PARAMETER_COOKIE */
    public static final String PARAMETER_COOKIE_TIMESTAMP = "timestamp";

    /** Reported when something invalid happened inside the server. */
    public static final String E_INTERNAL_ERROR = "e.internal_error";

    /** Reported when a user requests a service to which they do not have access. */
    public static final String E_ACCESS_DENIED = "e.access_denied";

    /** Reported when the client attempts to athenticate against an unbound version of an app. */
    public static final String E_NO_APP_VERSION = "e.no_app_version";

    /** Reported when the client attempts to athenticate with an expired session token. */
    public static final String E_SESSION_EXPIRED = "e.session_expired";

    /** Reported when the client attempts to athenticate as an unknown user. */
    public static final String E_NO_SUCH_USER = "e.no_such_user";

    /** Reported when the client attempts to athenticate with an invalid password. */
    public static final String E_INVALID_PASSWORD = "e.invalid_password";

    /** Reported when an account is created or updated with an invalid email address. */
    public static final String E_INVALID_EMAIL = "e.invalid_email";

    /** Reported when an account is created or updated with an in-use email address. */
    public static final String E_EMAIL_IN_USE = "e.email_in_use";
}
