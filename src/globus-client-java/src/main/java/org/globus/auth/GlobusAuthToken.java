/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Jan 3, 2016 by pruyne
 */

package org.globus.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth.HeaderType;
import org.apache.oltu.oauth2.common.OAuth.HttpMethod;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.globus.GlobusClientException;
import org.globus.GlobusEntity;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.jsonUtil.EnumDeserializer;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlobusAuthToken extends GlobusEntity
{

    @JsonProperty("access_token")
    public String tokenValue;

    @JsonProperty("expires_in")
    public Long tokenLife;

    @JsonProperty("id_token")
    public String idToken;

    @JsonProperty("resource_server")
    @JsonDeserialize(using = EnumDeserializer.class)
    public RequestType resourceServer;

    @JsonProperty("scope")
    public String scope;

    @JsonProperty("token_type")
    public String tokenType;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("other_tokens")
    public GlobusAuthToken[] otherTokens;

    // Additional fields from introspect

    @JsonProperty("username")
    public String username;

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("active")
    public Boolean active;

    @JsonProperty("nbf")
    public Long notBeforeTime;

    @JsonProperty("name")
    public String name;

    @JsonProperty("aud")
    public String[] audience;

    @JsonProperty("sub")
    public String subjectIdentity;

    @JsonProperty("iss")
    public String issuer;

    @JsonProperty("exp")
    public Long expirationTime;

    @JsonProperty("iat")
    public Long issuedTime;

    @JsonProperty("email")
    public String email;

    @JsonProperty("identities_set")
    public String[] identities;

    private String OAuthTokenUrl = null;
    private String OAuthClientCreds = null;


    public static GlobusAuthToken fromJson(String json) throws IOException
    {
        return GlobusEntity.fromJson(json, GlobusAuthToken.class);
    }


    /**
     * 
     * @param code
     * @param redirectUrl
     * @param OAuthTokenUrl
     * @param clientId
     * @param authScope
     * @param clientCreds
     * @return
     * @throws GlobusClientException
     */
    public static GlobusAuthToken exchangeForCode(String code, String redirectUrl,
                                                  String OAuthTokenUrl, String clientId,
                                                  String authScope, String clientCreds)
        throws GlobusClientException
    {
        try {
            OAuthClientRequest oauthRequest = OAuthClientRequest
                .tokenLocation(OAuthTokenUrl)
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setClientId(clientId)
                .setRedirectURI(redirectUrl)
                .setScope(authScope)
                .setCode(code)
                .buildBodyMessage();

            return OauthTokenRequest(OAuthTokenUrl, clientCreds, oauthRequest);
        } catch (Exception e) {
            throw new GlobusClientException("Exchange code for token failed: " + e);
        }
    }


    /**
     * 
     * @param OAuthTokenUrl
     * @param clientCreds
     * @param oauthRequest
     * @return
     * @throws GlobusClientException
     */
    private static GlobusAuthToken OauthTokenRequest(String OAuthTokenUrl, String clientCreds,
                                                     OAuthClientRequest oauthRequest)
        throws GlobusClientException
    {
        oauthRequest.setHeader(HeaderType.AUTHORIZATION, "Basic " + clientCreds);

        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

        OAuthAccessTokenResponse oAuthResponse;
        try {
            oAuthResponse = oAuthClient.accessToken(oauthRequest,
                                                    OAuthJSONAccessTokenResponse.class);
            String respBody = oAuthResponse.getBody();
            GlobusAuthToken token = fromJson(respBody);
            // If we just got back a token, then active must be true though it is not explicitly
            // set in the response in this case
            if (token.active == null) {
                token.active = true;
            }
            token.OAuthTokenUrl = OAuthTokenUrl;
            token.OAuthClientCreds = clientCreds;
            if (token.otherTokens != null) {
                for (GlobusAuthToken childToken : token.otherTokens) {
                    childToken.OAuthTokenUrl = OAuthTokenUrl;
                    childToken.OAuthClientCreds = clientCreds;
                }
            }
            return token;
        } catch (Exception e) {
            throw new GlobusClientException("Token request failed on " + e + " request was: " + 
                            oauthRequest.getBody());
        }

    }


    /**
     * 
     * @param OAuthTokenUrl
     * @param username
     * @param password
     * @param authScope
     * @param clientCreds
     * @return
     * @throws GlobusClientException
     */
    public static GlobusAuthToken passwordGrant(String OAuthTokenUrl, String username,
                                                String password, String authScope,
                                                String clientCreds)
        throws GlobusClientException
    {
        try {
            OAuthClientRequest oauthRequest = OAuthClientRequest
                .tokenLocation(OAuthTokenUrl)
                .setGrantType(GrantType.PASSWORD)
                .setUsername(username)
                .setPassword(password)
                .setScope(authScope)
                .buildBodyMessage();

            return OauthTokenRequest(OAuthTokenUrl, clientCreds, oauthRequest);

        } catch (Exception e) {
            throw new GlobusClientException("Password Grant failed: " + e);
        }
    }


    /**
     * @param bearerToken
     * @param type
     * @param oauthTokenUrl
     * @param clientCreds
     * @return
     */
    public static GlobusAuthToken fromBearer(String bearerToken, RequestType type,
                                             String oauthTokenUrl, String clientCreds)
    {
        GlobusAuthToken authToken = new GlobusAuthToken();
        authToken.tokenValue = bearerToken;
        authToken.resourceServer = type;
        authToken.OAuthClientCreds = clientCreds;
        authToken.OAuthTokenUrl = oauthTokenUrl;
        return authToken;
    }

    public static GlobusAuthToken fromStream(InputStream is) throws IOException
    {
        Scanner s = new Scanner(is);
        s.useDelimiter("\\A");
        GlobusAuthToken authToken = null;
        if (s.hasNext()) {
            authToken = fromJson(s.next());
        }
        is.close();
        s.close();
        return authToken;
    }


    public Map<RequestType, String> getTokenMap()
    {
        HashMap<RequestType, String> tokenMap = new HashMap<RequestType, String>();
        return createTokenMap(tokenMap);
    }


    /**
     * @param tokenMap
     * @return
     */
    private Map<RequestType, String> createTokenMap(HashMap<RequestType, String> tokenMap)
    {
        tokenMap.put(resourceServer, tokenValue);
        if (otherTokens != null) {
            for (GlobusAuthToken childToken : otherTokens) {
                childToken.createTokenMap(tokenMap);
            }
        }
        return tokenMap;
    }


    public GlobusAuthToken getAuthTokenForRequestType(RequestType type)
    {
        if (type == null || type.equals(resourceServer)) {
            return this;
        } else if (otherTokens != null) {
            for (GlobusAuthToken childToken : otherTokens) {
                GlobusAuthToken childLookup = childToken.getAuthTokenForRequestType(type);
                if (childLookup != null) {
                    return childLookup;
                }
            }
        }
        return null;
    }


    public GlobusAuthToken introspectDetails() throws GlobusClientException
    {
        OAuthClientRequest profileRequest;
        OAuthResourceResponse resp = null;
        String respBody = null;
        try {
            profileRequest = new OAuthBearerClientRequest(OAuthTokenUrl + "/introspect")
                .setAccessToken(tokenValue)
                .buildBodyMessage();
            // note: the builder defaults to 'access_token' whereas we require 'token'
            profileRequest.setBody("token=" + tokenValue + "&extra_fields=identities_set");

            profileRequest.setHeader(HeaderType.AUTHORIZATION, "Basic " + OAuthClientCreds);
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            resp =
                oAuthClient.resource(profileRequest, HttpMethod.POST, OAuthResourceResponse.class);
            respBody = resp.getBody();
            GlobusAuthToken fullToken = GlobusAuthToken.fromJson(respBody);
            // Copy all the property values from the introspection result on to this token
            copy(fullToken, true);
            if (otherTokens != null) {
                // If we have child tokens, we introspect them as well so that required details
                // are also populated on them
                for (GlobusAuthToken otherToken : otherTokens) {
                    otherToken.introspectDetails();
                }
            }
        } catch (OAuthSystemException | OAuthProblemException e) {
            throw new GlobusClientException("OAuth Operation failed " + e);
        } catch (IOException e) {
            throw new GlobusClientException("Unable to deserialize returned token body: "
                + respBody);
        }
        return this;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        final int maxLen = 3;
        return "GlobusAuthToken [" + (tokenValue != null ? "tokenValue=" + tokenValue + ", " : "")
            + (tokenLife != null ? "tokenLife=" + tokenLife + ", " : "")
            + (idToken != null ? "idToken=" + idToken + ", " : "")
            + (resourceServer != null ? "resourceServer=" + resourceServer + ", " : "")
            + (scope != null ? "scope=" + scope + ", " : "")
            + (tokenType != null ? "tokenType=" + tokenType + ", " : "")
            + (refreshToken != null ? "refreshToken=" + refreshToken + ", "
                : "")
            + (otherTokens != null ? "otherTokens="
                + Arrays.asList(otherTokens).subList(0, Math.min(otherTokens.length, maxLen)) + ", "
                : "")
            + (username != null ? "username=" + username + ", " : "")
            + (clientId != null ? "clientId=" + clientId + ", " : "")
            + (active != null ? "active=" + active + ", "
                : "")
            + (notBeforeTime != null
                ? "notBeforeTime=" + notBeforeTime
                    + ", "
                : "")
            + (name != null ? "name=" + name + ", " : "")
            + (audience != null
                ? "audience="
                    + Arrays.asList(audience).subList(0, Math.min(audience.length, maxLen)) + ", "
                : "")
            + (subjectIdentity != null ? "subjectIdentity=" + subjectIdentity + ", " : "")
            + (issuer != null ? "issuer=" + issuer + ", " : "")
            + (expirationTime != null ? "expirationTime=" + expirationTime + ", " : "")
            + (issuedTime != null ? "issuedTime=" + issuedTime + ", "
                : "")
            + (email != null ? "email=" + email + ", " : "")
            + (identities != null ? "identities="
                + Arrays.asList(identities).subList(0, Math.min(identities.length, maxLen)) + ", "
                : "")
            + (OAuthTokenUrl != null ? "OAuthTokenUrl=" + OAuthTokenUrl + ", " : "")
            + (OAuthClientCreds != null ? "OAuthClientCreds=" + OAuthClientCreds : "") + "]";
    }


}
