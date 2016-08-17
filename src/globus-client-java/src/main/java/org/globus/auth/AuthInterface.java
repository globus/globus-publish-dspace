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
package org.globus.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface;

/**
 * @author pruyne
 *
 */
public interface AuthInterface extends GlobusRestInterface
{
    public abstract GlobusUser getActiveUser() throws GlobusClientException;

    public abstract List<Identity> getIdentitiesForUsernames(String[] usernames, 
                                                             Collection<String> fields, 
                                                             boolean includeProviders) throws GlobusClientException;

    public abstract List<Identity> getIdentitiesForUserIds(String[] userids, 
                                                           Collection<String> fields, 
                                                           boolean includeProviders) throws GlobusClientException;
    
    public abstract Map<String,String> getUniqueIdForUserNames(String[] userNames) throws GlobusClientException;

    public abstract GlobusUser getUser(String username) throws GlobusClientException;

    public abstract GlobusUser getUser(String username, Collection<String> fields,
                                       Collection<String> customFields)
        throws GlobusClientException;
    
    public abstract GlobusAuthToken getAuthTokenForRequestType(RequestType type);    
    
}