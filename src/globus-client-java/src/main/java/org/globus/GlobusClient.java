/*
 * Copyright 2014 University of Chicago
 *
 * All Rights Reserved.
 */

/* Based on code carrying the following attribution:
 *
 *
 * *
 Copyright 2012 Johns Hopkins University Institute for Computational Medicine
 Copyright 2012 University of Chicago

 Based upon the GlobusOnline Nexus Client written in Python by Mattias Lidman
 available at https://github.com/globusonline/python-nexus-client

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.globus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.globus.auth.AuthInterface;
import org.globus.auth.GlobusAuthToken;
import org.globus.auth.GlobusUser;
import org.globus.auth.Identity;
import org.globus.auth.IdentityList;
import org.globus.groups.GlobusGroup;
import org.globus.groups.GlobusGroupMembership;
import org.globus.groups.GlobusGroupSummary;
import org.globus.groups.GroupMember;
import org.globus.groups.GroupMemberList;
import org.globus.groups.GroupsInterface;
import org.globus.jsonUtil.ClientCreation;
import org.globus.transfer.DeleteTask;
import org.globus.transfer.Endpoint;
import org.globus.transfer.RenameOperation;
import org.globus.transfer.SuccessfulTransfers;
import org.globus.transfer.Task;
import org.globus.transfer.TaskList;
import org.globus.transfer.TransferAccess;
import org.globus.transfer.TransferAccessList;
import org.globus.transfer.TransferDirectoryListing;
import org.globus.transfer.TransferInterface;
import org.globus.transfer.TransferOperation;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @author pruyne
 *
 */
public class GlobusClient
    implements GroupsInterface, AuthInterface, TransferInterface
{
    private static final Logger logger = Logger.getLogger(GlobusClient.class);

    @Deprecated
    protected static final String DEFAULT_NEXUS_HOST = "https://nexus.api.globusonline.org";
    @Deprecated
    protected static final String DEFAULT_TRANSFER_HOST =
        "https://transfer.api.globusonline.org/v0.10";

    private GlobusAuthToken authToken;

    private final Client restClient;

    @Deprecated
    protected String nexusHost;

    @Deprecated
    protected String transferHost;

    protected Map<RequestType, String> hostMap = null;

    protected boolean cachingEnabled = true;

    /*
     * The cache for group related operations. For now, we use a single big cache for all group
     * related operations. The key must encode the op as well as the value to lookup.
     */
    private LRUCacheWithTimeout<String, Object> groupOpCache;

    GlobusClient()
    {
        restClient = ClientCreation.createRestClient();
    }


    public GlobusClient(String username, String password, String scopes, String authTokenUrl,
                        String clientCreds)
        throws GlobusClientException
    {
        this(GlobusAuthToken.passwordGrant(authTokenUrl, username, password, scopes, clientCreds));
    }


    public GlobusClient(String accessTokenJson) throws IOException
    {
        this(GlobusAuthToken.fromJson(accessTokenJson));
    }


    public GlobusClient(GlobusAuthToken authToken)
    {
        this();
        this.authToken = authToken;
        authToken.setIntfImpl(this);
    }


    public GlobusClient(File accessTokenFile) throws IOException
    {
        this(new FileInputStream(accessTokenFile));
    }


    public GlobusClient(InputStream is) throws IOException
    {
        this(GlobusAuthToken.fromStream(is));
    }


    @Override
    public String getRootUrlForRequestType(RequestType requestType) throws GlobusClientException
    {
        if (hostMap != null) {
            String host = hostMap.get(requestType);
            if (host != null) {
                return host;
            } else {
                throw new GlobusClientException("No host has been assigned for request type: " + 
                                requestType);
            }
        } else {
            throw new GlobusClientException("GlobusClient: No hostmap has been created");
        }
    }


    @Override
    public String getBearerTokenForRequestType(RequestType requestType)
    {
        if (requestType == null) {
            return this.authToken.tokenValue;
        } else {
            return authToken.getTokenMap().get(requestType);
        }
    }
    
    @Override
    public GlobusAuthToken getAuthTokenForRequestType(RequestType requestType)
    {
        if (requestType == null) {
            return this.authToken;            
        } else {
            return authToken.getAuthTokenForRequestType(requestType);
        }
    }

    public boolean isCachingEnabled()
    {
        return cachingEnabled;
    }


    /**
     * @param cachingEnabled the cachingEnabled to set
     */
    public synchronized void setCachingEnabled(boolean cachingEnabled)
    {
        this.cachingEnabled = cachingEnabled;
        // Remove old cache if caching is being turned off
        if (!cachingEnabled && groupOpCache != null) {
            groupOpCache = null;
        }
    }


    private String generateJsonForRequestEntity(Object entity)
    {
        ObjectMapper mapper = new ObjectMapper();

        try {
            String json = mapper.writeValueAsString(entity);
            return json;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public <T> T doGet(RequestType requestType, String path, int desiredHttpResponse,
                       Map<String, Object> params, Class<T> responseClass)
        throws GlobusClientException
    {
        return doRestOp(RestMethod.GET, requestType, path, desiredHttpResponse, params, null,
                        responseClass);
    }


    @Override
    public <T> T doPost(RequestType requestType, String path, int desiredHttpResponse,
                        Map<String, Object> params, Object requestObj, Class<T> responseClass)
        throws GlobusClientException
    {
        return doRestOp(RestMethod.POST, requestType, path, desiredHttpResponse, params,
                        requestObj, responseClass);
    }


    @Override
    public <T> T doPut(RequestType requestType, String path, int desiredHttpResponse,
                       Map<String, Object> params, Object requestObj, Class<T> responseClass)
        throws GlobusClientException
    {
        return doRestOp(RestMethod.PUT, requestType, path, desiredHttpResponse, params, requestObj,
                        responseClass);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.GlobusRestInterface#doDelete(org.globus.GlobusRestInterface.RequestType,
     * java.lang.String, int, java.lang.Class)
     */
    @Override
    public <T> T doDelete(RequestType requestType, String path, int desiredHttpResponse,
                          Class<T> responseClass)
        throws GlobusClientException
    {
        return doRestOp(RestMethod.DELETE, requestType, path, desiredHttpResponse, null, null,
                        responseClass);
    }


    public static String utf8Encode(String input)
    {
        if (input == null) {
            return null;
        }
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // This should never happen on a sane JVM...
            logger.error("Cannot UTF-8 encode input string " + input, e);
            return null;
        }
    }


    /**
     * @param requestType TODO
     * @param path
     * @param desiredHttpResponse
     * @param params
     * @param responseClass
     * @return
     * @throws GlobusClientException
     */
    @Override
    public <T> T doRestOp(RestMethod method, RequestType requestType, String path,
                          int desiredHttpResponse, Map<String, Object> params,
                          Object requestEntity, Class<T> responseClass)
        throws GlobusClientException
    {
        // Temporary hack until NEXUS is completely obliterated as a type of request
        if (requestType.NEXUS.equals(requestType)) {
            requestType = RequestType.groups;
        }

        StringBuffer fullPath = new StringBuffer();
        fullPath.append(getRootUrlForRequestType(requestType));
        if (path == null) {
            throw new GlobusClientException("Input path to doRestOp is null");
        }
        if (!path.startsWith("/")) {
            fullPath.append("/");
        }
        fullPath.append(path);
        if (params != null && !params.isEmpty()) {
            Set<Entry<String, Object>> reqParam = params.entrySet();
            boolean firstTime = true;
            for (Entry<String, Object> paramEntry : reqParam) {
                Object paramVal = paramEntry.getValue();
                if (paramVal instanceof Object[]) {
                    // Convert an array to a collection for further processing
                    paramVal = Arrays.asList((Object[]) paramVal);
                }
                if (paramVal instanceof Collection) {
                    Collection<Object> coll = (Collection<Object>) paramVal;
                    if (!coll.isEmpty()) {
                        paramVal = commaSeparate(coll);
                    } else {
                        // If the collection is empty, we can ignore it
                        paramVal = null;
                    }
                }
                // Check first so that we don't assume we've added a param with the firstTime check
                if (paramVal == null) {
                    continue;
                }
                if (firstTime) {
                    firstTime = false;
                    fullPath.append("?");
                } else {
                    fullPath.append("&");
                }
                fullPath.append(paramEntry.getKey());
                fullPath.append("=");
                paramVal = utf8Encode(paramVal.toString());
                fullPath.append(paramVal.toString());
            }
        }
        WebResource resource = restClient.resource(fullPath.toString());
        resource.setProperty("REQUEST_TYPE", requestType);
        /*
         * String json = generateJsonForRequestEntity(requestEntity); if (json != null) {
         * System.out.println("Json to be output: " + json); }
         */

        ClientResponse response =
            resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + getBearerTokenForRequestType(requestType))
                .method(method.name(), ClientResponse.class, requestEntity);
        // resource.method(method.name(), ClientResponse.class, requestEntity);
        setJsonContentHeader(response);
        GlobusClientException gce = null;
        T entity = null;
        if (response.getStatus() == desiredHttpResponse) {
            try {
                entity = response.getEntity(responseClass);
                if (entity instanceof GlobusEntity[]) {
                    GlobusEntity[] ges = (GlobusEntity[]) entity;
                    for (GlobusEntity ge : ges) {
                        ge.setIntfImpl(this);
                    }
                } else if (entity instanceof GlobusEntity) {
                    GlobusEntity ge = (GlobusEntity) entity;
                    ge.setIntfImpl(this);
                }
            } catch (Exception e) {
                String errorMsg = "Failed to deserialize into  " + responseClass;
                logger.error(errorMsg, e);
                gce = new GlobusClientException(errorMsg);
                gce.otherException = e;
            }
        } else {
            gce = new GlobusClientException(response, fullPath.toString());
        }
        // If we've built up an exception of some sort, we throw it. Else, we return whatever entity
        // (perhaps null) that we've built up at this point.
        if (gce != null) {
            // gce.requestUrl = fullPath.toString();
            throw gce;
        }
        return entity;
    }


    /**
     * @param collection
     * @return
     */
    private String commaSeparate(Collection<Object> collection)
    {
        StringBuffer buf = new StringBuffer();
        boolean firstEntry = true;
        for (Object obj : collection) {
            if (firstEntry) {
                firstEntry = false;
            } else {
                buf.append(",");
            }
            buf.append(obj.toString());
        }

        return buf.toString();
    }


    @Override
    public <T> T doGet(RequestType requestType, String path, int desiredHttpResponse,
                       Class<T> responseClass)
        throws GlobusClientException
    {
        return doGet(requestType, path, desiredHttpResponse, null, responseClass);
    }


    /**
     * All parsing of the response requires that the data be interpreted as JSON. To insure that the
     * JSON Content-Type is set properly, the header is added if it isn't already there.
     * @param response A response to a previous REST request. It is guaranteed to have the
     *            application/json Content-Type header set upon return from this method.
     */
    private void setJsonContentHeader(ClientResponse response)
    {
        MultivaluedMap<String, String> headers = response.getHeaders();
        List<String> contentHeaders = headers.get("Content-Type");
        if (contentHeaders != null) {
            for (String header : contentHeaders) {
                if (header.contains(MediaType.APPLICATION_JSON)) {
                    return;
                }
            }
        }
        headers.remove("Content-Type");
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.NexusInterface#getActiveUser()
     */
    @Override
    public GlobusUser getActiveUser() throws GlobusClientException
    {
        return getUser(authToken.username);
    }


    @Override
    public List<Identity> getIdentitiesForUsernames(String[] usernames, Collection<String> fields,
                                                    boolean includeProviders)
        throws GlobusClientException
    {
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("fields[identity]", fields);
        if (includeProviders) {
            paramMap.put("include", "identity_provider");
        }
        paramMap.put("usernames", usernames);
        IdentityList idList =
            doGet(RequestType.auth, "/identities", 200, paramMap, IdentityList.class);

        return Arrays.asList(idList.identities);
    }


    @Override
    public List<Identity> getIdentitiesForUserIds(String[] userids, Collection<String> fields,
                                                  boolean includeProviders)
        throws GlobusClientException
    {
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("fields[identity]", fields);
        if (includeProviders) {
            paramMap.put("include", "identity_provider");
        }
        paramMap.put("ids", userids);
        IdentityList idList =
            doGet(RequestType.auth, "/identities", 200, paramMap, IdentityList.class);

        return Arrays.asList(idList.identities);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.globus.auth.AuthInterface#getUniqueIdForUserNames(java.lang.String[])
     */
    @Override
    public Map<String, String> getUniqueIdForUserNames(String[] userNames)
        throws GlobusClientException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("identity_names", userNames);
        Object retObj = doGet(RequestType.auth, "/identities/ids", 200, paramMap, Object.class);
        if (retObj instanceof Map) {
            return (Map<String, String>) retObj;
        } else {
            return null;
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.NexusInterface#getUser(java.lang.String)
     */
    @Override
    public GlobusUser getUser(String username) throws GlobusClientException
    {
        return getUser(username, null, null);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.NexusInterface#getUser(java.lang.String, java.util.Collection,
     * java.util.Collection)
     */
    @Override
    public GlobusUser getUser(String username, Collection<String> fields,
                              Collection<String> customFields)
        throws GlobusClientException
    {
        // # If no fields are explicitly set the following will be returned by
        // Graph:
        // # ['fullname', 'email', 'username', 'email_validated',
        // 'system_admin', 'opt_in']
        // # No custom fields are returned by default.
        StringBuffer url = new StringBuffer();
        url.append("/users/");
        url.append(utf8Encode(username));
        HashMap<String, Object> paramMap = null;
        if (fields != null && !fields.isEmpty()) {
            paramMap = new HashMap<String, Object>();
            paramMap.put("fields", fields);
        }
        GlobusUser user =
            doGet(RequestType.groups, url.toString(), 200, paramMap, GlobusUser.class);

        return user;
    }


    private String cacheKeyForOpAndKeys(String opName, Object... opKeys)
    {
        StringBuffer buf = new StringBuffer(opName);
        for (Object keyVal : opKeys) {
            buf.append("::");
            buf.append(keyVal != null ? keyVal.toString() : "<null>");
        }
        return buf.toString();
    }


    private synchronized <T> T getFromGroupOpCache(Class<? extends T> inClass, String opName,
                                                   Object... opKeys)
    {
        if (!cachingEnabled) {
            return null;
        }
        if (opName == null || opKeys == null) {
            return null;
        }
        if (groupOpCache == null) {
            // If no cache has been created, no use checking
            return null;
        }
        String cacheKey = cacheKeyForOpAndKeys(opName, opKeys);
        Object result = groupOpCache.get(cacheKey);
        try {
            @SuppressWarnings("unchecked")
            T typedResult = (T) result;
            return typedResult;
        } catch (Exception e) {
            // We ignore and return null since the cast failed
        }
        return null;
    }


    private synchronized void cacheGroupOpResult(Object result, String opName, Object... opKeys)
    {
        if (opName == null || opKeys == null || !cachingEnabled || result == null) {
            return;
        }
        if (groupOpCache == null) {
            groupOpCache = new LRUCacheWithTimeout<>();
            // 30 seconds is the recommended cache time for Nexus operations
            groupOpCache.setMaxLifetime(30 * 1000);
            groupOpCache.setMaxSize(200); // Max 200 results saved
        }
        String cacheKey = cacheKeyForOpAndKeys(opName, opKeys);
        groupOpCache.put(cacheKey, result);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.NexusInterface#getAllGroups()
     */
    @Override
    public org.globus.groups.GlobusGroupSummary[] getAllGroups() throws GlobusClientException
    {
        GlobusGroupSummary[] groups =
            getFromGroupOpCache(GlobusGroupSummary[].class, "allGroups", "");
        if (groups == null) {
            groups = doGet(RequestType.groups, "/groups", 200, GlobusGroupSummary[].class);
            cacheGroupOpResult(groups, "allGroups", "");
        }
        return groups;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.NexusInterface#getGroupById(java.lang.String)
     */
    @Override
    public GlobusGroup getGroupById(String id) throws GlobusClientException
    {
        if (id == null) {
            return null;
        }
        GlobusGroup group = getFromGroupOpCache(GlobusGroup.class, "group", id);
        if (group == null) {
            group = doGet(RequestType.groups, "/groups/" + id, 200, GlobusGroup.class);
            if (group != null) {
                cacheGroupOpResult(group, "group", id);
            }
        }
        return group;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.nexus.NexusInterface#getMembersForGroup(java.lang.String)
     */
    @Override
    public Collection<GroupMember> getMembersForGroup(String groupId,
                                                      EnumSet<MembershipRole> roleInGroup,
                                                      EnumSet<MembershipStatus> statusInGroup,
                                                      String userName)
        throws GlobusClientException
    {
        if (groupId == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Collection<GroupMember> members =
            getFromGroupOpCache(Collection.class, "membersForGroup", groupId, roleInGroup,
                                statusInGroup, userName);
        if (members == null) {
            StringBuffer path = new StringBuffer("/groups/");
            path.append(groupId);
            path.append("/members");
            if (userName != null) {
                path.append("/");
                path.append(userName);
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("role", roleInGroup);
            params.put("status", statusInGroup);
            if (userName == null) {
                GroupMemberList memberList =
                    doGet(RequestType.groups, path.toString(), 200, params, GroupMemberList.class);
                if (memberList != null) {
                    members = Arrays.asList(memberList.members);
                    for (GroupMember member : members) {
                        member.setIntfImpl(this);
                    }
                }
            } else {
                // If the username is not null, we'll get back only the one user requested, not a
                // list
                GroupMember member =
                    doGet(RequestType.groups, path.toString(), 200, GroupMember.class);
                members = Arrays.asList(member);
            }
            // Is this right: it says that if a group has no members, we don't cache. Perhaps we
            // shouldn't care since an empty group is a valid return.
            if (members.size() > 0) {
                cacheGroupOpResult(members, "membersForGroup", groupId, roleInGroup, statusInGroup,
                                   userName);
            }
        }
        return members;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.nexus.NexusInterface#getMembersForGroup(java.lang.String)
     */
    @Override
    public List<String> getMembersForGroup(String groupId) throws GlobusClientException
    {
        @SuppressWarnings("unchecked")
        List<String> memberNames = getFromGroupOpCache(List.class, "getMembersForGroup", groupId);
        if (memberNames == null) {
            java.util.Collection<GroupMember> members =
                getMembersForGroup(groupId, null, null, null);
            int numMembers = members.size();
            memberNames = new ArrayList<>(numMembers);
            for (GroupMember member : members) {
                memberNames.add(member.username);
            }
            cacheGroupOpResult(memberNames, "getMembersForGroup", groupId);
        }
        return memberNames;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.NexusInterface#getGroupsForUser(org.globus.nexus.NexusUser)
     */
    @Override
    public Collection<GlobusGroupMembership> getGroupsForUser(GlobusUser user)
        throws GlobusClientException
    {
        return getGroupsForUser(user.username);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.NexusInterface#getGroupsForUser(java.lang.String)
     */
    @Override
    public Collection<GlobusGroupMembership> getGroupsForUser(String userName)
        throws GlobusClientException
    {
        GlobusUser userWithGroups =
            getFromGroupOpCache(GlobusUser.class, "groupsForUser", userName);
        if (userWithGroups == null) {
            userWithGroups = getUser(userName, Arrays.asList("groups"), null);
            cacheGroupOpResult(userWithGroups, "groupsForUser", userName);
        }
        List<GlobusGroupMembership> groupsForUser = Arrays.asList(userWithGroups.groups);

        return groupsForUser;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.globus.groups.GroupsInterface#getGroups(java.util.EnumSet, java.util.EnumSet,
     * boolean, boolean, org.globus.nexus.GlobusGroupSummary)
     */
    @Override
    public GlobusGroupSummary[] getGroups(EnumSet<MembershipRole> myRoles,
                                          EnumSet<MembershipStatus> myStatuses,
                                          boolean forAllIdenities,
                                          boolean includeIdentitySetProperties,
                                          GlobusGroupSummary filter)
        throws GlobusClientException
    {
        Map<String, Object> params = new HashMap<>();
        String filters = createFilterListForTemplateObject(filter);
        GlobusGroupSummary[] summaries = null;

        summaries = getFromGroupOpCache(GlobusGroupSummary[].class, "groups", myRoles, myStatuses,
                                        forAllIdenities, includeIdentitySetProperties, filters);
        if (summaries != null) {
            return summaries;
        }

        if (filters != null) {
            params.put("filter", filters);
        }
        params.put("my_roles", myRoles);
        params.put("my_statuses", myStatuses);
        if (forAllIdenities) {
            params.put("for_all_identities", true);
        }
        if (includeIdentitySetProperties) {
            params.put("include_identity_set_properties", true);
        }
        summaries = doGet(RequestType.groups, "/groups", 200, params, GlobusGroupSummary[].class);
        if (summaries != null) {
            cacheGroupOpResult(summaries, "groups", myRoles, myStatuses, forAllIdenities,
                               includeIdentitySetProperties, filters);
        }
        return summaries;
    }


    public GlobusGroupSummary[] getGroupsForCurrentUser()
        throws GlobusClientException
    {
        EnumSet<MembershipStatus> statuses = EnumSet.allOf(MembershipStatus.class);
        EnumSet<MembershipRole> roles = EnumSet.allOf(MembershipRole.class);

        GlobusGroupSummary[] summaries = getGroups(roles, statuses, true, true, null);
        return summaries;
    }


    public synchronized String setRootUrlForRequestType(RequestType reqType, String host)
    {
        if (hostMap == null) {
            hostMap = new HashMap<RequestType, String>();
        }
        if (reqType != null && host != null) {
            return hostMap.put(reqType, host);
        } else {
            return null;
        }
    }


    public synchronized void setRequestTypeRootUrls(Map<RequestType, String> hostMap)
    {
        if (this.hostMap != null) {
            this.hostMap.clear();
        }
        for (Entry<RequestType, String> entry : hostMap.entrySet()) {
            setRootUrlForRequestType(entry.getKey(), entry.getValue());
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#activateEndpoint(java.lang.String)
     */
    @Override
    public boolean activateEndpoint(String endPoint) throws GlobusClientException
    {
        String path = null;
        path = "/endpoint/" + utf8Encode(endPoint) + "/activate";

        Object result = doPost(RequestType.transfer, path, 200, null, null, Object.class);
        // System.out.println("doPost result: " + result);
        return result != null;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#lsEndpoint(java.lang.String)
     */
    @Override
    public TransferDirectoryListing lsDirectory(String endPoint, String directory)
        throws GlobusClientException
    {
        Map<String, Object> dirParam = new HashMap<>();
        dirParam.put("path", directory);
        String path = "/endpoint/" + utf8Encode(endPoint) + "/ls";
        TransferDirectoryListing dir =
            doGet(RequestType.transfer, path, 200, dirParam, TransferDirectoryListing.class);
        return dir;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#getEndpoint(java.lang.String)
     */
    @Override
    public Endpoint getEndpoint(String endpointName) throws GlobusClientException
    {
        Endpoint transferEndpoint =
            doGet(RequestType.transfer, "/endpoint/" + utf8Encode(endpointName), 200,
                  Endpoint.class);
        return transferEndpoint;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#createDirectory(java.lang.String)
     */
    @Override
    public boolean createDirectory(String endpointName, String directoryName)
        throws GlobusClientException
    {
        TransferOperation top = TransferOperation.mkdir(directoryName);

        Object putResult =
            doPost(RequestType.transfer, "/endpoint/" + utf8Encode(endpointName) + "/mkdir", 202,
                   null, top, Object.class);
        return true;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#setEndpointSharing(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean setEndpointSharing(String endpoint, String sharePath, String sharingName,
                                      String sharingComment)
        throws GlobusClientException
    {
        TransferOperation top =
            TransferOperation.sharedEndpoint(endpoint, sharePath, sharingName, sharingComment);

        Object shareResult =
            doPost(RequestType.transfer, "/shared_endpoint", 201, null, top, Object.class);
        return true;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#setEndpointAccessPermissions(java.lang.String,
     * java.lang.String, org.globus.transfer.TransferInterface.PrincipalType, java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean setEndpointAccessPermissions(String endpointName, String path,
                                                PrincipalType principalType, String principalName,
                                                String principalPerms, boolean editable)
        throws GlobusClientException
    {
        TransferOperation top =
            TransferOperation.access(path, principalType, principalName, principalPerms);
        String endpointPath;
        endpointPath = "endpoint/" + utf8Encode(endpointName) + "/access";
        Object accessResult =
            doPost(RequestType.transfer, endpointPath, 201, null, top, Object.class);
        return true;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#getAccessList(java.lang.String)
     */
    @Override
    public TransferAccessList getAccessList(String endpointName) throws GlobusClientException
    {
        TransferAccessList accessList =
            doGet(RequestType.transfer, "endpoint/" + utf8Encode(endpointName) + "/access_list",
                  200, TransferAccessList.class);
        return accessList;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#deleteAccess(java.lang.String,
     * org.globus.transfer.TransferAccess)
     */
    @Override
    public void deleteAccess(String endpointName, TransferAccess accessItem)
        throws GlobusClientException
    {
        doDelete(RequestType.transfer, "endpoint/" + utf8Encode(endpointName) + "/access/"
            + accessItem.id, 200, Object.class);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#taskList()
     */
    @Override
    public TaskList taskList() throws GlobusClientException
    {
        return taskList(null, null, null, null);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#taskList(org.globus.transfer.Task,
     * org.globus.transfer.Task, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public TaskList taskList(Task filterVals, Task resultFields, Integer offset, Integer limit)
        throws GlobusClientException
    {
        Map<String, Object> params = new HashMap<>();

        if (offset != null) {
            params.put("offset", offset);
        }
        if (limit != null) {
            params.put("limit", limit);
        }

        Set<String> resultFieldList = createResultFieldListForTemplateObj(resultFields);
        if (resultFieldList != null) {
            params.put("fields", resultFieldList);
        }

        String filterList = createFilterListForTemplateObject(filterVals);
        if (filterList != null) {
            params.put("filter", filterList);
        }

        TaskList rVal = doGet(RequestType.transfer, "/task_list", 200, params, TaskList.class);
        return rVal;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.globus.transfer.TransferInterface#taskList(org.globus.transfer.Task,
     * org.globus.transfer.Task, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public SuccessfulTransfers successfulTransfers(String transferId, Integer offset, Integer limit)
        throws GlobusClientException
    {
        Map<String, Object> params = new HashMap<>();

        if (offset != null) {
            params.put("offset", offset);
        }
        if (limit != null) {
            params.put("limit", limit);
        }

        SuccessfulTransfers rVal =
            doGet(RequestType.transfer, "/task/" + transferId + "/successful_transfers", 200,
                  params, SuccessfulTransfers.class);
        return rVal;
    }


    @Override
    public String createSubmissionId() throws GlobusClientException
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> submissionObj =
            doGet(RequestType.transfer, "/submission_id", 200, Map.class);
        String submissionId = null;
        if (submissionObj != null) {
            Object retType = submissionObj.get("DATA_TYPE");
            Object valueObj = submissionObj.get("value");
            if (valueObj != null) {
                submissionId = valueObj.toString();
            }
        }
        return submissionId;
    }


    @Override
    public Task deletePathAsync(String endpoint, String path, boolean recursive,
                                boolean notifyOnSuccess, boolean notifyOnFailure)
        throws GlobusClientException
    {
        String submissionId = createSubmissionId();
        if (submissionId == null) {
            throw new GlobusClientException("Could not create a submission id for delete task");
        }
        DeleteTask dt = new DeleteTask(submissionId, endpoint, path, recursive);
        // We'll assume for a nice API call that we don't want notification on this
        dt.notifyOnFailure = notifyOnFailure;
        dt.notifyOnSuccess = notifyOnSuccess;

        Task task = doPost(RequestType.transfer, "/delete", 202, null, dt, Task.class);
        return task;
    }


    @Override
    public boolean deletePath(String endpoint, String path, boolean recursive)
        throws GlobusClientException
    {
        Task task = deletePathAsync(endpoint, path, recursive, false, false);
        return Task.Status.SUCCEEDED.equals(task.waitForStatus(10000, Task.Status.SUCCEEDED,
                                                               Task.Status.FAILED));
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.globus.transfer.TransferInterface#rename(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public boolean rename(String srcEndpoint, String srcPath, String destPath)
        throws GlobusClientException
    {
        RenameOperation ro = new RenameOperation(srcPath, destPath);
        String path = "/operation/endpoint/" + utf8Encode(srcEndpoint) + "/rename";
        Object rVal = doPost(RequestType.transfer, path, 200, null, ro, Object.class);
        return rVal != null;

    }


    /**
     * @param filterVals
     */
    private String createFilterListForTemplateObject(Object obj)
    {
        Map<Field, Object> fields = getNonNullFieldVals(obj);
        if (fields != null && !fields.isEmpty()) {
            StringBuffer filterVal = new StringBuffer();
            boolean firstTime = true;
            for (Entry<Field, Object> field : fields.entrySet()) {
                String jsonName = getJsonNameForField(field.getKey());
                if (!firstTime) {
                    filterVal.append("/");
                } else {
                    firstTime = false;
                }
                filterVal.append(jsonName);
                filterVal.append(":");
                filterVal.append(field.getValue().toString());
            }
            return filterVal.toString();
        }
        return null;
    }


    /**
     * @param obj
     */
    private Set<String> createResultFieldListForTemplateObj(Object obj)
    {
        Map<Field, Object> fields = getNonNullFieldVals(obj);
        if (fields != null && !fields.isEmpty()) {
            Set<String> jsonPropNames = new HashSet<>();
            for (Field field : fields.keySet()) {
                String jsonName = getJsonNameForField(field);
                if (jsonName != null) {
                    jsonPropNames.add(jsonName);
                }
            }
            return jsonPropNames;
        }
        return null;
    }


    /**
     * @param field
     * @return
     */
    private String getJsonNameForField(Field field)
    {
        JsonProperty jsonProp = field.getAnnotation(JsonProperty.class);
        if (jsonProp == null) {
            return field.getName();
        }
        String propVal = jsonProp.value();
        if (propVal == null || propVal.isEmpty()) {
            return field.getName();
        }
        return propVal;
    }


    /**
     * @param resultFields
     * @return
     */
    private Map<Field, Object> getNonNullFieldVals(Object testObject)
    {
        if (testObject == null) {
            return null;
        }
        Field[] fieldList = testObject.getClass().getFields();
        Map<Field, Object> fields = new HashMap<>();
        for (Field field : fieldList) {
            try {
                Object fieldVal = field.get(testObject);
                if (fieldVal != null) {
                    fields.put(field, fieldVal);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // We'll treat this as if it is null
            }
        }

        return fields;
    }


    /**
     * @param paramMap
     * @param paramName
     * @param optionSet
     */
    private void addParamForOptions(Map<String, Object> paramMap, String paramName,
                                    EnumSet<? extends Enum> optionSet)
    {
        if (paramMap == null || paramName == null || optionSet == null || optionSet.isEmpty()) {
            return;
        }
        StringBuffer buf = new StringBuffer();
        boolean firstTime = true;

        for (Enum option : optionSet) {
            if (firstTime) {
                firstTime = false;
            } else {
                buf.append(',');
            }
            String name = option.name();
            buf.append(name);
        }
        paramMap.put(paramName, buf.toString());
    }
}
