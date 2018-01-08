/**
 * This file is a modified version of a DSpace file.
 * All modifications are subject to the following copyright and license.
 * 
 * Copyright 2016 University of Chicago. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.globus;

import java.sql.SQLException;
import java.util.Date;
import java.util.EnumSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.auth.GlobusAuthToken;
import org.globus.groups.GlobusGroupSummary;
import org.globus.groups.GroupsInterface.MembershipRole;
import org.globus.groups.GroupsInterface.MembershipStatus;


/**
 * Authentication module for GlobusAuth based authentication. Based on the LDAP and Shib. authentication
 * modules shipped with DSpace.
 */

public class GlobusAuthAuthentication implements AuthenticationMethod
{

    /** log4j category */
    private static final org.apache.log4j.Logger log = Logger.getLogger(GlobusAuthAuthentication.class);

    /**
     * Let a real auth method return true if it wants.
     */
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username)
        throws SQLException
    {
        // We will create a user (EPerson) in DSpace if it doesn't currently exist upon successful
        // login.
        return true;
    }


    /**
     * Nothing here, initialization is done when auto-registering.
     */
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson)
        throws SQLException
    {
        /*
         * This is do nothing since we set all this in the authorize method
         *
         * if (eperson != null) { // TODO: This is obviously not right yet...
         * eperson.setNetid(request.getParameter("GlobusUserName")); }
         */
    }


    /**
     * Cannot change Globus password from DSpace
     */
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username)
        throws SQLException
    {
        return false;
    }


    /**
     * This is an explicit method.
     */
    public boolean isImplicit()
    {
        return false;
    }


    /**
     * Add authenticated users to the group defined in dspace.cfg by the login.specialgroup key.
     */
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        return new int[0];
    }

   
    /**
     * Authenticate the given credentials. This is the heart of the authentication method: test the
     * credentials for authenticity, and if accepted, attempt to match (or optionally, create) an
     * <code>EPerson</code>. If an <code>EPerson</code> is found it is set in the
     * <code>Context</code> that was passed.
     *
     * @param context DSpace context, will be modified (ePerson set) upon success.
     *
     * @param userName Username (or email address) when method is explicit. Use null for implicit
     *            method.
     *
     * @param password Password for explicit auth, or null for implicit method.
     *
     * @param realm Realm is an extra parameter used by some authentication methods, leave null if
     *            not applicable.
     *
     * @param request The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of: SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     *         <p>
     *         Meaning: <br>
     *         SUCCESS - authenticated OK. <br>
     *         BAD_CREDENTIALS - user exists, but credentials (e.g. passwd) don't match <br>
     *         CERT_REQUIRED - not allowed to login this way without X.509 cert. <br>
     *         NO_SUCH_USER - user not found using this method. <br>
     *         BAD_ARGS - user/pw not appropriate for this method
     */
    public int authenticate(Context context, String userName, String password,
                            String realm, HttpServletRequest request) throws SQLException
    {
        log.info("Globus Auth login");
        
        GlobusAuthToken authToken = (GlobusAuthToken)request.getAttribute("globusAuthToken");
        
        Globus g = new Globus(authToken);
        GlobusClient gc = g.getClient();

        try
        {
            authToken.introspectDetails();
        }
        catch (GlobusClientException e1)
        {
            return BAD_ARGS;
        }

    	String email = authToken.email;

    	EPerson eperson = null;
    	try {
    	    eperson = getEPersonForAuthToken(context, authToken, 
    	            canSelfRegister(context, request, email), request);
    	} catch (AuthorizeException ae) {
    	    // Still null so handled below
    	}

        // if they entered a netid that matches an eperson
        if (eperson != null) {
            log.info("EPerson exists - trying to auth");

            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate()) {
                return CERT_REQUIRED;
            } else if (!eperson.canLogIn()) {
                return BAD_ARGS;
            }
            eperson.setLastActive(new Date());
            try {
                context.setIgnoreAuthorization(true);
                eperson.update();
                context.commit();
            } catch (Exception e) {
                log.error("ERROR updating user " + email + " with access token", e);
                return DUPLICATE_EMAIL;
            } finally {
                context.setIgnoreAuthorization(false);
            }
            context.setCurrentUser(eperson);
            assignGroups(gc, context);

            return SUCCESS;
        }
        return BAD_ARGS;
    }


    /**
     * Return the EPerson object associated with an AuthToken which has already
     * been introspected to get user details.
     * @param context
     * @param authToken The authtoken carrying user information from introspection
     * @param createIfNotPresent If true, a new user will be created in the system
     * for the authtoken details if not already present. This requires a context
     * which is not in READ_ONLY mode.
     * @param request The input HttpServletRequest that triggered this action.
     * It is needed to pass on to AuthenticationManager.initEPerson in the case
     * where a new user is created. 
     * @return An EPerson object for the user presenting the authtoken
     * @throws SQLException If DB lookup or update fails
     * @throws AuthorizeException If authorization to create the new user
     * fails. New user creation takes place in a region of authorization checks
     * turned off, so this should not ever happen.
     */
    public static EPerson getEPersonForAuthToken(Context context, 
            GlobusAuthToken authToken, boolean createIfNotPresent,
            HttpServletRequest request) throws SQLException, AuthorizeException
    {
        String effectiveIdentity = authToken.subjectIdentity;

        EPerson eperson = null;

        // Locate the eperson
        log.info("Trying to get user by netid " + effectiveIdentity);
        eperson = EPerson.findByNetid(context, effectiveIdentity);
        if (eperson != null) {
            return eperson;
        }

        if (createIfNotPresent) {            
            try {
                String displayName = authToken.name;
                String email = authToken.email;
                if (email == null) {
                    email = authToken.username;
                }
                log.info("User doesn't exist - trying to auto register: " + authToken.username);
                String firstName = "";
                String lastName = "";
                int spaceSepLoc = displayName.indexOf(" ");
                if (spaceSepLoc > 0) {
                    firstName = displayName.substring(0, spaceSepLoc).trim();
                    lastName = displayName.substring(spaceSepLoc).trim();
                } else {
                    firstName = displayName;
                }
                context.turnOffAuthorisationSystem();
                eperson = EPerson.create(context);

                eperson.setEmail(email);
                eperson.setFirstName(firstName);
                eperson.setLastName(lastName);
                eperson.setNetid(effectiveIdentity);
                eperson.setCanLogIn(true);
                if (request != null) {
                    AuthenticationManager.initEPerson(context, request, eperson);
                }
                eperson.update();
                context.commit();
                context.restoreAuthSystemState();
            } catch (AuthorizeException e) {
                log.error("User authentication failed on ", e);
                throw e;
            } catch (IllegalStateException ise) {
                log.info("Failed to create a new user due to context state");
                throw ise;
            } catch (Exception e) {
                log.error("Error creating new user", e);
                throw e;
            } finally {
                context.setIgnoreAuthorization(false);
            }
        }
        return eperson;
    }

    /*
     * Returns URL to which to redirect to obtain credentials (either password prompt or e.g. HTTPS
     * port for client cert.); null means no redirect.
     *
     * @param context DSpace context, will be modified (ePerson set) upon success.
     *
     * @param request The HTTP request that started this operation, or null if not applicable.
     *
     * @param response The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
    public String loginPageURL(Context context, HttpServletRequest request,
                               HttpServletResponse response)
    {
        return GlobusAuth.getAuthRedirectURL(request);
    }


    /**
     * Returns message key for title of the "login" page, to use in a menu showing the choice of
     * multiple login methods.
     *
     * @param context DSpace context, will be modified (ePerson set) upon success.
     *
     * @return Message key to look up in i18n message catalog.
     */
    public String loginPageTitle(Context context)
    {
        return "org.globus.dspace.eperson.LDAPAuthentication.title";
    }
    
    private void assignGroups(GlobusClient client, Context context)
    {
    	log.info("Checking admin group membership");
    	
    	// set up the search filter and retrieve active memberships
    	GlobusGroupSummary [] groups = null;
    	EnumSet<MembershipStatus> statuses = EnumSet.of(MembershipStatus.active);
    	EnumSet<MembershipRole> roles = EnumSet.of(MembershipRole.admin, MembershipRole.manager, MembershipRole.member);

        try{
        	groups  = client.getGroups(roles, statuses, true, true, null);
        } catch(Exception e){
        	log.error("Exception getting groups" + e);
        	e.printStackTrace();
        }
        
        Group adminDSpaceGroup;

        try{
        	adminDSpaceGroup = Group.find(context, 1);	// group 1 is the administrators group
        } catch (Exception e){
        	log.error("Cant find dspace admin group");
        	return;
        }

        // remove user from admin group (if they're in it)
        if (adminDSpaceGroup.isMember(context.getCurrentUser())){
        	log.info("User is current member of admin group -- removing admin");
        	try{
	        	adminDSpaceGroup.removeMember(context.getCurrentUser());
	        	adminDSpaceGroup.update();
	        	context.commit();
        	 } catch (Exception e) {
                log.error("Exception removing user from admin group ", e);
             }

        }

        // Get configured admin group
        String adminGp = ConfigurationManager.getProperty("globus", "admingroup");
        if (adminGp == null)
        {
        	return;
        }

        try {          
            for (GlobusGroupSummary group : groups) {
       		    if (adminGp.equals(group.id)){
       		    	adminDSpaceGroup.addMember(context.getCurrentUser());
                	adminDSpaceGroup.update();
                    context.commit();
                    log.info("Added user to admin group");
       		    	break;
       		    }
            }
        } catch (Exception e) {
            log.error("Exception mapping groups ", e);
        }
    }
}
