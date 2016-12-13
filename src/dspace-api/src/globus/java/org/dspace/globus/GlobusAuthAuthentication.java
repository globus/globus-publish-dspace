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

    	String displayName = authToken.name;
    	String email = authToken.email;
    	String effectiveIdentity = authToken.subjectIdentity;

        EPerson eperson = null;

        // Locate the eperson
        log.info("Trying to get user by netid " + effectiveIdentity);
        try {
            eperson = EPerson.findByNetid(context, effectiveIdentity);
        } catch (Exception e) {
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
        } else {
            log.info("User doesnt exist - trying to auto register" + email);

            if (canSelfRegister(context, request, email)) {
                // TEMPORARILY turn off authorization
                try {
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
                    context.restoreAuthSystemState();

                    eperson.setEmail(email);
                    eperson.setFirstName(firstName);
                    eperson.setLastName(lastName);
                    eperson.setNetid(effectiveIdentity);
                    eperson.setLastActive(new Date());
                    eperson.setCanLogIn(true);
                    AuthenticationManager.initEPerson(context, request, eperson);
                } catch (AuthorizeException e) {
                    log.error("User authentication failed on ", e);
                    return NO_SUCH_USER;
                } catch (Exception e) {
                    log.error("Error creating new user", e);
                } finally {
                    context.setIgnoreAuthorization(false);
                }
            }
        }
        
        if (eperson != null) {
            try {
                context.setIgnoreAuthorization(true);
                eperson.update();
                context.commit();
            } catch (Exception e) {
                log.error("ERROR updating user with access token", e);
                e.printStackTrace();
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
