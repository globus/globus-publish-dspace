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
package org.globus.groups;

import java.util.Collection; 
import java.util.EnumSet;
import java.util.List;

import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface;
import org.globus.auth.GlobusUser;

/**
 * @author pruyne
 *
 */
public interface GroupsInterface extends GlobusRestInterface
{

    public static enum MembershipStatus {
        active,
        invited,
        pending,
        suspended,
        rejected;
    }

    public static enum MembershipRole {
        admin,
        manager,
        member,
    	user;
    }


    public abstract GlobusUser getActiveUser() throws GlobusClientException;

    public abstract GlobusUser getUser(String username) throws GlobusClientException;


    public abstract GlobusUser getUser(String username, Collection<String> fields,
                                       Collection<String> customFields)
        throws GlobusClientException;


    public abstract GlobusGroupSummary[] getAllGroups() throws GlobusClientException;

    public abstract GlobusGroupSummary[] getGroups(EnumSet<MembershipRole> myRoles, EnumSet<MembershipStatus> myStatuses, boolean forAllIdenities, boolean includeIdentitySetProperties, GlobusGroupSummary filter) throws GlobusClientException;
    

    public abstract GlobusGroup getGroupById(String id) throws GlobusClientException;


    /**
     * Find the members of a group adhering to the input filters.
     * @param groupId The id of the group
     * @param roleInGroup A set of roles for the members of interest.
     * @param statusInGroup A set of statuses for the members of interest.
     * @param userName A specific user to check for membership in the group.
     * @return A collection of group membership status. If the collection is empty no users matching
     *         the criteria were found (including the case where a single user was searched for and
     *         that user is not a member of the group).
     * @throws GlobusClientException If an error occurs including the group with the provided
     *             groupId not existing.
     */
    public abstract Collection<GroupMember> getMembersForGroup(String groupId,
                                                               EnumSet<MembershipRole> roleInGroup,
                                                               EnumSet<MembershipStatus> statusInGroup,
                                                               String userName)
        throws GlobusClientException;


    /**
     * A simplified version of the getMembersForGroup that provides no filtering and passes back
     * only user names, not coplete infomration about the group membership.
     * @param groupId The id of the group
     * @return A list of strings containing user names in the group. An empty list indicates no
     *         members in the group.
     * @throws GlobusClientException If any error occurs including a non-existent name provided for
     *             groupId.
     */
    public abstract List<String> getMembersForGroup(String groupId)
        throws GlobusClientException;


    public abstract Collection<GlobusGroupMembership> getGroupsForUser(GlobusUser user)
        throws GlobusClientException;


    public abstract Collection<GlobusGroupMembership> getGroupsForUser(String userName)
        throws GlobusClientException;
}