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
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.globus.Globus;

/**
 * Servlet that logs out any current user if invoked.
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class LogoutServlet extends DSpaceServlet
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(LogoutServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "logout", ""));

        Authenticate.loggedOut(context, request);

        // if the user still logged in (i.e. it was a login as)?
        if (context.getCurrentUser() != null)
        {
            // redirect to the admin home page
            response.sendRedirect(request.getContextPath()+"/dspace-admin/");
            return;
        }

        // Log out of Globus too and redirect back again
        response.sendRedirect(Globus.getGlobusLogoutLink(request));

        //JSPManager.showJSP(request, response, "/login/logged-out.jsp");
    }
}
