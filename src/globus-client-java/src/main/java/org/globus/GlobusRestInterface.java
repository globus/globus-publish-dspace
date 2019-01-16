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

import java.util.Map;

/**
 * @author pruyne
 *
 */
public interface GlobusRestInterface
{
    public enum RequestType {
        @Deprecated
        NEXUS("nexus"),
        transfer("transfer.api.globus.org"),
        groups("nexus.api.globus.org"),
        auth("auth.globus.org"),
        atmosphere("atmosphere"),
        publish("publish"),
        search("search.api.globus.org"),
        identifiers("identifiers.globus.org"),
        xsede("xsede");

        private String typeName;
        public String getTypeName()
        {
            return typeName;
        }

        private RequestType(String typeName) {
            this.typeName = typeName;
        }
        
        public static RequestType forTypeName(String typeName) {
            RequestType[] values = RequestType.values();
            for (RequestType requestType : values) {
                if (requestType.typeName.equals(typeName)) {
                    return requestType;
                }
            }
            return null;
        }
    }

    public enum RestMethod {
        GET,
        PUT,
        POST,
        DELETE
    }


    public <T> T doGet(RequestType requestType, String path, int desiredHttpResponse,
                       Class<T> responseClass) throws GlobusClientException;


    public <T> T doRestOp(RestMethod method, RequestType requestType, String path,
                          int desiredHttpResponse, Map<String, Object> params,
                          Object requestEntity, Class<T> responseClass)
        throws GlobusClientException;


    public <T> T doPost(RequestType requestType, String path, int desiredHttpResponse,
                        Map<String, Object> params, Object requestObj, Class<T> responseClass)
        throws GlobusClientException;


    public <T> T doPut(RequestType requestType, String path, int desiredHttpResponse,
                       Map<String, Object> params, Object requestObj, Class<T> responseClass)
        throws GlobusClientException;


    public <T> T doGet(RequestType requestType, String path, int desiredHttpResponse,
                       Map<String, Object> params, Class<T> responseClass)
        throws GlobusClientException;


    public <T> T doDelete(RequestType requestType, String path, int desiredHttpResponse,
                          Class<T> responseClass) throws GlobusClientException;


    public String getRootUrlForRequestType(RequestType requestType) throws GlobusClientException;
    
    public String getBearerTokenForRequestType(RequestType requestType);
}
