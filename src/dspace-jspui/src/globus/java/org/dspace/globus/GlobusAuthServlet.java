/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.globus;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.globus.auth.GlobusAuthToken;

/**
 * Globus Auth authentication servlet. 
 *
 * @author Jim Pryune/Kyle Chard
 * @version $Revision$
 */
public class GlobusAuthServlet extends DSpaceServlet
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(GlobusAuthServlet.class);

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.info("Globus Auth Servlet GET");
        String code = request.getParameter("code");
        
        // handle redirect
        if (code != null)
        {
            try
            {
                log.info("Recieved code:  " + code);
                GlobusAuthToken authToken = GlobusAuth.getAuthTokenFromCode(request, code);
                // The following log should never be enabled in production as it stores a user's
                // token, but it can be useful in debugging so leave it in commented out.
//                log.info("Globus Tokens: " + tokens);
                request.setAttribute("globusAuthToken",authToken);
                int status = AuthenticationManager.authenticate(context,
                		null, null, null, request);
                
                if (status == AuthenticationMethod.SUCCESS)
                {
                    // Logged in OK.
                    Authenticate.loggedIn(context, request,
                            context.getCurrentUser());

                    // Store the token on the session so it can be re-used on
                    // future requests. Note that Authenticate.loggedIn may have
                    // just created a new session, so we have to do this after
                    // the called to loggedIn above else it may get lost when
                    // the new session is created.
                    // TODO - need to store the token in the session
                    Globus.addTokenToHttpSession(request, authToken);

                    // We still have to do this copy since the loggedIn method
                    // couldn't since it didn't have access to the token
                    Globus.copyTokenRequestToContext(request, context);

                    // Set the Locale according to user preferences
                    Locale epersonLocale = I18nUtil.getEPersonLocale(context
                            .getCurrentUser());
                    context.setCurrentLocale(epersonLocale);
                    Config.set(request.getSession(), Config.FMT_LOCALE,
                            epersonLocale);

                    log.info(LogManager.getHeader(context, "login",
                            "type=globusauth"));

                    // resume previous request
                    Authenticate.resumeInterruptedRequest(request, response);

                    return;
                }
                else if (status == AuthenticationMethod.DUPLICATE_EMAIL){
                	JSPManager.showJSP(request, response,
                            "/login/goauth-duplicate-email.jsp");
                    return;
                }
                else 
                {
                    JSPManager.showJSP(request, response,
                            "/login/goauth-incorrect.jsp");
                    return;
                }

            }
            catch (Exception e)
            {
                log.warn("Error " + e);
                JSPManager.showJSP(request, response,
                        "/login/goauth-incorrect.jsp");
                return;
            }

        }

        JSPManager.showJSP(request, response, "/login/globusauth.jsp");
    }

}
