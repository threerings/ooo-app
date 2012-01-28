//
// $Id$

package com.threerings.app.client;

import com.threerings.app.data.AppCodes;

/**
 * Communicates an error to a GWT client.
 */
public class ServiceException extends Exception
{
    /**
     * Throws a Service with the given message unless condition is true.
     */
    public static void unless (boolean condition, String message)
        throws ServiceException
    {
        if (!condition) {
            throw new ServiceException(message);
        }
    }

    /** Returns a service exception with the {@link AppCodes#E_INTERNAL_ERROR} code. */
    public static ServiceException internalError () {
        return new ServiceException(AppCodes.E_INTERNAL_ERROR);
    }

    /** Returns a service exception with the {@link AppCodes#E_ACCESS_DENIED} code. */
    public static ServiceException accessDenied () {
        return new ServiceException(AppCodes.E_ACCESS_DENIED);
    }

    /** Returns a service exception with the {@link AppCodes#E_SESSION_EXPIRED} code. */
    public static ServiceException sessionExpired () {
        return new ServiceException(AppCodes.E_SESSION_EXPIRED);
    }

    /**
     * Creates a service exception with the supplied translation message.
     */
    public ServiceException (String message)
    {
        super(message);
    }

    /**
     * Default constructor for use when unserializing.
     */
    public ServiceException ()
    {
    }
}
