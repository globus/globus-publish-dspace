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
package org.globus.jsonUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.globus.GlobusRestInterface.RequestType;
import org.globus.auth.GlobusAuthToken;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * @author pruyne
 *
 */
@Deprecated
public class GlobusAuthFilter extends ClientFilter
{
    Map<RequestType, String> tokenMap = null;

    public GlobusAuthFilter(Map<RequestType, String> tokens)
    {
        for (Entry<RequestType, String> entry : tokens.entrySet()) {
            addToTokenMap(entry.getKey(), entry.getValue());
        }
    }


    public GlobusAuthFilter(File accessTokenFile) throws IOException
    {
        this(new FileInputStream(accessTokenFile));
    }


    public GlobusAuthFilter(InputStream is) throws IOException
    {
        Properties props = new Properties();
        props.load(is);
        tokenMap = new HashMap<RequestType, String>();
        for (Entry<Object, Object> entry : props.entrySet()) {
            addToTokenMap(entry.getKey(), entry.getValue().toString());
        }
    }

    /**
     * @param authToken
     */
    public GlobusAuthFilter(GlobusAuthToken authToken)
    {
        tokenMap = authToken.getTokenMap();
    }


    private synchronized void addToTokenMap(Object key, String val)
    {
        if (tokenMap == null) {
            tokenMap = new HashMap<RequestType, String>();
        }
        RequestType reqType = null;
        if (key instanceof RequestType) {
            reqType = (RequestType) key;
        } else if (key != null) {
            reqType = RequestType.valueOf(key.toString());
            if (reqType == null) {
                reqType = RequestType.forTypeName(key.toString());
                }
        }
        if (reqType != null) {
            tokenMap.put(reqType, val);
        }
    }
    
    public String getAccessToken(RequestType reqType) {
        if (tokenMap != null) {
            return tokenMap.get(reqType);            
        }
        return null;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see
     * com.sun.jersey.api.client.filter.ClientFilter#handle(com.sun.jersey.api.client.ClientRequest)
     */
    @Override
    public ClientResponse handle(ClientRequest cr) throws ClientHandlerException
    {
        Map<String, Object> reqProps = cr.getProperties();
        Object requestType = reqProps.get("REQUEST_TYPE");
        if (requestType instanceof RequestType) {
            RequestType req = (RequestType) requestType;
            String token = getAccessToken(req);
            if (token != null) {
                String authString =  "Bearer "+ token;            
                cr.getHeaders().add("Authorization", authString);
            }
        }
        return getNext().handle(cr);
    }
}
