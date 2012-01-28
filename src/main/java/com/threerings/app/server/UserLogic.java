//
// $Id$

package com.threerings.app.server;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.net.MailUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.servlet.user.InvalidUsernameException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.Username;

import com.samskivert.depot.DuplicateKeyException;

import com.threerings.user.ExternalAuther;
import com.threerings.user.OOOUser;
import com.threerings.user.depot.DepotUserRepository;
import com.threerings.user.depot.ExternalAuthRepository;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;

/**
 * Provides user services.
 */
@Singleton
public class UserLogic
{
    /**
     * Effects the logon of the specified user. Returns a session token for the user if
     * authentication succeeds.
     *
     * @exception ServiceException thrown if authentication fails. See {@link AppCodes} for
     * possible failure codes.
     */
    public String logon (String username, String password, int sessionDays)
        throws ServiceException
    {
        OOOUser user = _userRepo.loadUser(username, false); // TODO: by email?
        if (user == null) {
            throw new ServiceException(AppCodes.E_NO_SUCH_USER);
        }
        if (!user.passwordsMatch(Password.makeFromCrypto(password))) {
            throw new ServiceException(AppCodes.E_INVALID_PASSWORD);
        }
        return _userRepo.registerSession(user, sessionDays);
    }

    /**
     * Checks that the supplied session token is valid and if so, refreshes it for the specified
     * number of days. Returns false if the token is invalid or has expired.
     */
    public boolean refreshSession (String authtok, int sessionDays)
        throws ServiceException
    {
        return StringUtil.isBlank(authtok) ? false : _userRepo.refreshSession(authtok, sessionDays);
    }

    /**
     * Returns the user that is authenticated via the supplied token (may be null) or null.
     *
     * @exception ServiceException thrown if an internal error occurs trying to locate the user
     * data.
     */
    public OOOUser getUser (String authtok)
    {
        return StringUtil.isBlank(authtok) ? null : _userRepo.loadUserBySession(authtok, false);
    }

    /**
     * Returns the specified user's external user id and their most recently saved session key.
     * Returns null if the user has no external data mapping and the session key may be null if
     * they have no recently stored session key.
     */
    public Tuple<String, String> getExtAuthInfo (ExternalAuther auther, int userId)
    {
        return _extAuthRepo.loadExternalAuthInfo(auther, userId);
    }

    /**
     * Returns a mapping from external user id to local user id for all users in the supplied list.
     * Users that have no local account will be omitted from the mapping.
     */
    public Map<String, Integer> mapExtAuthIds (ExternalAuther auther, List<String> externalIds)
    {
        return _extAuthRepo.loadUserIds(auther, externalIds);
    }

    /**
     * Updates one or more of a user's email address, password and realname. Apps may wish to
     * provide a mechanism whereby a user can configure an email address or password so as to allow
     * direct authentication with the app. They also may obtain the user's real name from an
     * external site and can communicate that back to the app to make support procedures easier.
     *
     * @param userId the id of the user whose data will be updated.
     * @param email a new email address for the user in question, or null to leave it unchanged.
     * @param password a new password for the user, or null to leave it unchanged.
     * @param realname the user's real name or null to leave it unchanged.
     *
     * @exception ServiceException thrown if the update fails. See {@link AppCodes} for possible
     * failure codes.
     */
    public void updateUser (int userId, String email, Password password, String realname)
        throws ServiceException
    {
        OOOUser user = _userRepo.loadUser(userId);
        if (user == null) {
            throw new ServiceException(AppCodes.E_NO_SUCH_USER);
        }

        try {
            if (email != null && !email.equals(user.email)) {
                user.setUsername(new AppUsername(email));
                user.setEmail(email);
            }
        } catch (InvalidUsernameException iue) {
            throw new ServiceException(AppCodes.E_INVALID_EMAIL);
        }
        if (password != null && !password.getEncrypted().equals(user.password)) {
            user.setPassword(password);
        }
        if (realname != null && !realname.equals(user.realname)) {
            user.setRealName(realname);
        }

        try {
            _userRepo.updateUser(user);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(AppCodes.E_EMAIL_IN_USE);
        }
    }

    /**
     * Creates a new OOOUser record with the supplied credentials.
     *
     * @return the id of the newly created user.
     *
     * @exception ServiceException thrown with {@link AppCodes#E_INVALID_EMAIL} if the supplied
     * email is not a valid string or {@link AppCodes#E_EMAIL_IN_USE} if the email is in use by
     * another account.
     */
    public int createUser (String email, String password)
        throws ServiceException
    {
        try {
            return _userRepo.createUser(new AppUsername(email), password, email, 0);
        } catch (InvalidUsernameException iue) {
            throw new ServiceException(AppCodes.E_INVALID_EMAIL);
        } catch (UserExistsException uee) {
            throw new ServiceException(AppCodes.E_EMAIL_IN_USE);
        }
    }

    protected static class AppUsername extends Username
    {
        public AppUsername (String username) throws InvalidUsernameException {
            super(username);
        }
        @Override
        protected void validateName (String username) throws InvalidUsernameException {
            if (!MailUtil.isValidAddress(username)) {
                throw new InvalidUsernameException("Not a valid email address");
            }
        }
    }

    @Inject protected DepotUserRepository _userRepo;
    @Inject protected ExternalAuthRepository _extAuthRepo;
}
