//
// $Id$

package com.threerings.app.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;

/**
 * A base servlet for apps that wish to make use of authentication and other user services.
 */
public class AppServlet extends HttpServlet
{
    /**
     * Returns the user that made the current HTTP request or null if no auth token was supplied
     * with the request.
     *
     * @return the OOOUser record for that user or null.
     */
    protected OOOUser getUser (HttpServletRequest req)
    {
        return _servletLogic.getUser(req);
    }

    /**
     * Requires that the requester have a valid session.
     *
     * @return the OOOUser record for that user.
     * @exception ServiceException thrown with {@link AppCodes#E_SESSION_EXPIRED} if no valid
     * session is available.
     */
    protected OOOUser requireUser (HttpServletRequest req, HttpServletResponse rsp)
        throws ServiceException
    {
        OOOUser user = getUser(req);
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
    protected OOOUser requireAdmin (HttpServletRequest req, HttpServletResponse rsp)
        throws ServiceException
    {
        OOOUser user = requireUser(req, rsp);
        if (!user.isAdmin()) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }
        return user;
    }

    @Inject protected ServletLogic _servletLogic;
    @Inject protected UserLogic _userLogic;
}
