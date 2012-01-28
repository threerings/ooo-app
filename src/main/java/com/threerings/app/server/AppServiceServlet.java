//
// $Id$

package com.threerings.app.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;
import com.threerings.app.server.ServletLogic;

import static com.threerings.app.Log.log;

/**
 * A base servlet for app GWT service implementations that simplifies handling of authentication,
 * and other basic services.
 */
public class AppServiceServlet extends RemoteServiceServlet
{
    @Override // from RemoteServiceServlet
    public String processCall (String payload)
        throws SerializationException
    {
        try {
            return super.processCall(payload);
        } finally {
            _perThreadUser.remove();
        }
    }

    @Override
    protected void doUnexpectedFailure (Throwable error)
    {
        HttpServletRequest req = getThreadLocalRequest();
        HttpServletResponse rsp = getThreadLocalResponse();

        // if this is an "unexpected exception", unwrap the inner exception
        if (error instanceof UnexpectedException) {
            error = error.getCause();
        }

        // log the failure to the application log
        log.warning("Service request failure", "uri", req.getRequestURI(), error);

        // in case we failed while writing the response, we need to reset the response (from
        // RemoteServiceServlet)
        try {
            rsp.reset();
        } catch (IllegalStateException ex) {
            // If we can't reset the request, the only way to signal that something has gone wrong
            // is to throw an exception from here. It should be the case that we call the user's
            // implementation code before emitting data into the response, so the only time that
            // gets tripped is if the object serialization code blows up.
            throw new RuntimeException("Unable to report failure", error);
        }

        // send a standard failure response
        try {
            rsp.setContentType("text/plain");
            rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                rsp.getOutputStream().write(GENERIC_FAILURE_MSG.getBytes("UTF-8"));
            } catch (IllegalStateException ex) {
                // Handle the (unexpected) case where getWriter() was previously used
                rsp.getWriter().write(GENERIC_FAILURE_MSG);
            }
        } catch (IOException ioe) {
            log.warning("Failed to write failure response", ioe);
        }
    }

    /**
     * Effects the logon of the specified user, setting a cookie in the HTTP response with their
     * credentials if successful.
     *
     * @exception ServiceException thrown if the supplied credentials are invalid. See {@link
     * AppCodes} for potential error codes.
     */
    protected void logon (String username, String password, int sessionDays)
        throws ServiceException
    {
        _servletLogic.logon(getThreadLocalRequest(), getThreadLocalResponse(),
                            username, password, sessionDays);
    }

    /**
     * Returns the user that made the current HTTP request or null if no auth token was supplied
     * with the request.
     *
     * @return the OOOUser record for that user or null.
     */
    protected OOOUser getUser ()
    {
        return _perThreadUser.get();
    }

    /**
     * Requires that the requester have a valid session.
     *
     * @return the OOOUser record for that user.
     * @exception ServiceException thrown with {@link AppCodes#E_SESSION_EXPIRED} if no valid
     * session is available.
     */
    protected OOOUser requireUser ()
        throws ServiceException
    {
        OOOUser user = getUser();
        if (user == null) {
            throw new ServiceException(AppCodes.E_SESSION_EXPIRED);
        }
        return user;
    }

    /**
     * Requires that the requester have a valid session and that they have admin privileges.
     *
     * @return the OOOUser record for that user.
     * @exception ServiceException thrown with {@link AppCodes#E_SESSION_EXPIRED} if no valid
     * session is available, {@link AppCodes#E_ACCESS_DENIED} if the requester is not an admin.
     */
    protected OOOUser requireAdmin ()
        throws ServiceException
    {
        OOOUser user = requireUser();
        if (!user.isAdmin()) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }
        return user;
    }

    /** Provides the OOOUser corresponding to the current request. */
    protected transient ThreadLocal<OOOUser> _perThreadUser = new ThreadLocal<OOOUser>() {
        @Override protected OOOUser initialValue () {
            return _servletLogic.getUser(getThreadLocalRequest());
        }
    };

    @Inject protected ServletLogic _servletLogic;

    // used by doUnexpectedFailure
    protected static final String GENERIC_FAILURE_MSG =
        "The call failed on the server; see server log for details";
}
