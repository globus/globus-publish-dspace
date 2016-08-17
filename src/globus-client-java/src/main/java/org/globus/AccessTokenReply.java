/*
 * Copyright 2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.globus;

import java.util.Arrays;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class AccessTokenReply
{

    private String[] scopes;
    private String accessTokenHash;
    private Date issuedOn;
    private long expiry;
    private String tokenType;
    private String clientId;
    private long lifetime;
    private String accessToken;
    private long expiresIn;
    private String tokenId;
    private String userName;
    private String refreshToken;


    /**
     * @return the scopes
     */
    @JsonProperty("scopes")
    public String[] getScopes()
    {
        return scopes;
    }


    /**
     * @param scopes the scopes to set
     */
    public void setScopes(String[] scopes)
    {
        this.scopes = scopes;
    }


    /**
     * @return the accessTokenHash
     */
    @JsonProperty("access_token_hash")
    public String getAccessTokenHash()
    {
        return accessTokenHash;
    }


    /**
     * @param accessTokenHash the accessTokenHash to set
     */
    public void setAccessTokenHash(String accessTokenHash)
    {
        this.accessTokenHash = accessTokenHash;
    }


    /**
     * @return the issuedOn
     */
    @JsonProperty("issued_on")
    public Date getIssuedOn()
    {
        return issuedOn;
    }


    /**
     * @param issuedOn the issuedOn to set
     */
    public void setIssuedOn(Date issuedOn)
    {
        this.issuedOn = issuedOn;
    }


    /**
     * @return the expiry
     */
    @JsonProperty("expiry")
    public long getExpiry()
    {
        return expiry;
    }


    /**
     * @param expiry the expiry to set
     */
    public void setExpiry(long expiry)
    {
        this.expiry = expiry;
    }


    /**
     * @return the tokenType
     */
    @JsonProperty("token_type")
    public String getTokenType()
    {
        return tokenType;
    }


    /**
     * @param tokenType the tokenType to set
     */
    public void setTokenType(String tokenType)
    {
        this.tokenType = tokenType;
    }


    /**
     * @return the clientId
     */
    @JsonProperty("client_id")
    public String getClientId()
    {
        return clientId;
    }


    /**
     * @param clientId the clientId to set
     */
    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }


    /**
     * @return the lifetime
     */
    @JsonProperty("lifetime")
    public long getLifetime()
    {
        return lifetime;
    }


    /**
     * @param lifetime the lifetime to set
     */
    public void setLifetime(long lifetime)
    {
        this.lifetime = lifetime;
    }


    /**
     * @return the accessToken
     */
    @JsonProperty("access_token")
    public String getAccessToken()
    {
        return accessToken;
    }


    /**
     * @param accessToken the accessToken to set
     */
    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }


    /**
     * @return the expiresIn
     */
    @JsonProperty("expires_in")
    public long getExpiresIn()
    {
        return expiresIn;
    }


    /**
     * @param expiresIn the expiresIn to set
     */
    public void setExpiresIn(long expiresIn)
    {
        this.expiresIn = expiresIn;
    }


    /**
     * @return the tokenId
     */
    @JsonProperty("token_id")
    public String getTokenId()
    {
        return tokenId;
    }


    /**
     * @param tokenId the tokenId to set
     */
    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }


    /**
     * @return the userName
     */
    @JsonProperty("user_name")
    public String getUserName()
    {
        return userName;
    }


    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }


    @JsonProperty("refresh_token")
    public String getRefreshToken()
    {
        return refreshToken;

    }


    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "AccessTokenReply [scopes=" + Arrays.toString(scopes) + ", accessTokenHash="
            + accessTokenHash + ", issuedOn=" + issuedOn + ", expiry=" + expiry + ", tokenType="
            + tokenType + ", clientId=" + clientId + ", lifetime=" + lifetime + ", accessToken="
            + accessToken + ", expiresIn=" + expiresIn + ", tokenId=" + tokenId + ", userName="
            + userName + "]";
    }

}
