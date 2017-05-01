/*******************************************************************************
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
 *******************************************************************************/
package org.dspace.globus;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.dspace.core.ConfigurationManager;
import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.auth.GlobusAuthToken;

public class GlobusAuth
{
    private static final Logger log = Logger.getLogger(GlobusAuth.class);

    public static final String GLOBUS_AUTH_CONFIG_MODULE = "globus-auth";

    public static final String OAUTH_PATH = "/oauth-login";

    public static final String GLOBUS_AUTH_CLIENT_CREDS = "globus.client.base64.creds";

    public static final String GLOBUS_AUTH_SCOPE = "globus.auth.scope";

    public static final String GLOBUS_AUTH_USER_SCOPE = "globus.user.scope";

    public static final String GLOBUS_AUTH_OAUTH_URL = "globus.oauth.url";

    public static final String GLOBUS_AUTH_AUTH_PATH = "globus.auth.path";

    public static final String GLOBUS_AUTH_TOKEN_PATH = "globus.token.path";

    public static final String GLOBUS_AUTH_TOKEN_INTROSPECT_PATH = "globus.token.introspect.path";

    public static String getGlobusAuthConfigProperty(String propName)
    {
        String propVal = ConfigurationManager
                .getProperty(GLOBUS_AUTH_CONFIG_MODULE, propName);
        if (propVal == null)
        {
            log.error("No configuration value found for property " + propName);
        }
        return propVal;
    }

    public static String getAuthRedirectURL(HttpServletRequest request)
    {
        try
        {
            String redirect_url = Globus.getPublishURL(request) + OAUTH_PATH;
            OAuthClientRequest oauthRequest = OAuthClientRequest
                    .authorizationLocation(
                            getGlobusAuthConfigProperty(GLOBUS_AUTH_OAUTH_URL)
                                    + getGlobusAuthConfigProperty(
                                            GLOBUS_AUTH_AUTH_PATH))
                    .setClientId(
                            getGlobusAuthConfigProperty(Globus.GLOBUS_AUTH_CLIENT_ID))
                    .setScope(getGlobusAuthConfigProperty(GLOBUS_AUTH_SCOPE))
                    .setRedirectURI(redirect_url).setResponseType("code")
                    .buildQueryMessage();

            return oauthRequest.getLocationUri();
        }
        catch (Exception e)
        {
            log.error("OAuth error: " + e);
            e.printStackTrace();
        }
        return null;
    }

    public static GlobusAuthToken getAuthTokenFromCode(
            HttpServletRequest request, String code)
    {
        try
        {
            String redirect_url = Globus.getPublishURL(request) + OAUTH_PATH;

            String oauthTokenUrl = getGlobusAuthConfigProperty(
                    GLOBUS_AUTH_OAUTH_URL)
                    + getGlobusAuthConfigProperty(GLOBUS_AUTH_TOKEN_PATH);
            String clientId = getGlobusAuthConfigProperty(
                    Globus.GLOBUS_AUTH_CLIENT_ID);
            String scope = getGlobusAuthConfigProperty(GLOBUS_AUTH_SCOPE);
            String clientCreds = getGlobusAuthConfigProperty(
                    GLOBUS_AUTH_CLIENT_CREDS);
            return GlobusAuthToken.exchangeForCode(code, redirect_url,
                    oauthTokenUrl, clientId, scope, clientCreds);
        }
        catch (Exception e)
        {
            log.error("OAuth error: " + e);
            return null;
        }
    }
    
    public static GlobusAuthToken getAuthTokenFromUsernamePassword(String username, String password)
    {
        String oauthTokenUrl = getGlobusAuthConfigProperty(
                GLOBUS_AUTH_OAUTH_URL)
                + getGlobusAuthConfigProperty(GLOBUS_AUTH_TOKEN_PATH);
        String scope = getGlobusAuthConfigProperty(GLOBUS_AUTH_SCOPE);
        String clientCreds = getGlobusAuthConfigProperty(
                GLOBUS_AUTH_CLIENT_CREDS);
        try
        {
            return GlobusAuthToken.passwordGrant(oauthTokenUrl, username, password, scope, clientCreds);
        }
        catch (GlobusClientException e)
        {
            log.error("Password Grant returned: " + e);
            return null;
        }
    }

    public static GlobusAuthToken getAuthTokenFromClientCredentials(String clientId, String clientSecret)
    {
        String oauthTokenUrl = getGlobusAuthConfigProperty(
                GLOBUS_AUTH_OAUTH_URL)
                + getGlobusAuthConfigProperty(GLOBUS_AUTH_TOKEN_PATH);
        String scope = getGlobusAuthConfigProperty(GLOBUS_AUTH_SCOPE);
        String clientCreds = getGlobusAuthConfigProperty(
                GLOBUS_AUTH_CLIENT_CREDS);
        try
        {
            return GlobusAuthToken.clientCredentialGrant(oauthTokenUrl, clientId, clientSecret, scope, clientCreds);
        }
        catch (GlobusClientException e)
        {
            log.error("ClientCredential Grant returned: " + e);
            return null;
        }
    }
}
