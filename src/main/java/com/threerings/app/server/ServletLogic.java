//
// ooo-app - a simple framework for (Java-based) social webapps
// Copyright (c) 2012, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/ooo-app/blob/master/etc/LICENSE

package com.threerings.app.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.user.ExternalAuther;
import com.threerings.user.OOOUser;
import com.threerings.user.depot.DepotUserRepository;
import com.threerings.user.depot.ExternalAuthRepository;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;

import static com.threerings.app.Log.log;

/**
 * Provides services to servlets.
 */
@Singleton
public class ServletLogic
{
    /**
     * Extracts the authentication for the supplied request.
     */
    public String getAuthCookie (HttpServletRequest req)
    {
        return ServletAuthUtil.getAuthCookie(req);
    }

    /**
     * Extracts the authentication token from the supplied request, loads and returns the user
     * associated with that token, or null.
     *
     * @return the user that made this HTTP request or null.
     */
    public OOOUser getUser (HttpServletRequest req)
    {
        return getUser(getAuthCookie(req));
    }

    /**
     * Loads and returns the user associated with the token, or null.
     *
     * @return the user that supplied this auth code or null.
     */
    public OOOUser getUser (String authCode)
    {
        return _userLogic.getUser(authCode);
    }

    /**
     * Returns the external auther's id for a given OOOUser's id or null if there's no entry for
     * the user.
     */
    public String getExternalId (ExternalAuther auther, int userId)
    {
        Tuple<String, String> authInfo = _extAuthRepo.loadExternalAuthInfo(auther, userId);
        return authInfo == null ? null : authInfo.left;
    }

    /**
     * Effects the logon of the specified user. Configures an auth token cookie into the supplied
     * servlet response if authentication succeeds.
     *
     * @exception ServiceException thrown if authentication fails. See {@link AppCodes} for
     * possible failure codes.
     */
    public void logon (HttpServletRequest req, HttpServletResponse rsp,
                       String username, String password, int sessionDays)
        throws ServiceException
    {
        String authtok = _userLogic.logon(username, password, sessionDays);
        ServletAuthUtil.addAuthCookie(req, rsp, authtok, sessionDays);
    }

    /**
     * Registers the authentication of the given user id.
     *
     * @return the auth token for the user's session.
     *
     * @exception ServiceException thrown if we attempt to create a new user with the external
     * credentials and that process fails. See {@link AppCodes} for possible failure codes.
     */
    public String externalLogon (ExternalAuther auther, String id, String sessionKey)
        throws ServiceException
    {
        int userId = _extAuthRepo.loadUserIdForExternal(auther, id);
        if (userId == 0) {
            // create a new account for this user
            log.info("Creating a new user for external auther", "auther", auther, "id", id);
            userId = _userLogic.createUser(auther.makeEmail(id), "");
            // create an external mapping for this account and auther
            _extAuthRepo.mapExternalAccount(userId, auther, id, sessionKey);
        } else {
            // update the external session key on file for this user
            _extAuthRepo.updateExternalSession(auther, id, sessionKey);
        }
        // start an app session for this user
        OOOUser user = _userRepo.loadUser(userId);
        ServiceException.require(user != null, "Missing user for external logon [extId=" + id +
                                 ", userId=" + userId + "]");
        return _userRepo.registerSession(user, EXT_AUTH_DAYS);
    }

    /**
     * Validates that our external session is active and valid and recaptures the external session
     * key for the authenticated user.
     *
     * @return true if everything was good to go, false if something's not set up and the caller
     * needs to invoke {@link #externalLogon} to set up the external session.
     */
    public boolean refreshExternalSession (
        ExternalAuther auther, String extId, String extSessionKey,
        HttpServletRequest req, HttpServletResponse rsp)
    {
        if (StringUtil.isBlank(extId)) {
            return false; // we have no external session at all, nothing doing
        }

        // determine which user the external auther thinks we are and which we think we are
        int userId = _extAuthRepo.loadUserIdForExternal(auther, extId);
        OOOUser user = getUser(req);
        if (user == null || user.userId != userId) {
            return false; // if they don't match, we need to reauth
        }

        // otherwise we're all set, so update the external session key on file for this user
        _extAuthRepo.updateExternalSession(auther, extId, extSessionKey);

        // TEMP: refresh their auth cookie in case it is an old improperly domained cookie
        String authtok = CookieUtil.getCookieValue(req, AppCodes.AUTH_COOKIE);
        if (authtok != null) { // shouldn't be possible to be non-null but whatevs
            ServletAuthUtil.addAuthCookie(req, rsp, authtok, -1);
        }
        return true;
    }

    /**
     * Associates (creating, if necessary) an OOOUser account with a given external authenticator
     * ID and auth token. This is used in cases where a user arrived as a guest and was
     * authenticated at a point where a full externalLogon is undesireable. Future calls to getUser
     * with an HTTP request with the given authtok cookie will return the account linked here.
     *
     * @exception ServiceException thrown if we attempt to create a new user with the external
     * credentials and that process fails. See {@link AppCodes} for possible failure codes.
     */
    public void authorizeForExternalSession (ExternalAuther auther, String extId,
                                             String sessionKey, String authtok)
        throws ServiceException
    {
        int userId = _extAuthRepo.loadUserIdForExternal(auther, extId);
        if (userId != 0) {
            _extAuthRepo.updateExternalSession(auther, extId, sessionKey);
        } else {
            log.info("Creating new user from " + auther, "extId", extId);
            userId = _userLogic.createUser(auther.makeEmail(extId), "");
            _extAuthRepo.mapExternalAccount(userId, auther, extId, sessionKey);
        }
        _userRepo.setSession(userId, authtok, EXT_AUTH_DAYS);
    }

    @Inject protected DepotUserRepository _userRepo;
    @Inject protected ExternalAuthRepository _extAuthRepo;
    @Inject protected UserLogic _userLogic;

    protected static final int EXT_AUTH_DAYS = 2;
}
