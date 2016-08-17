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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.globus.FromChildDeserializer;
import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.JsonChildPath;
import org.globus.groups.GroupsInterface.MembershipRole;

/**
 * @author pruyne
 *
 */
// @JsonIgnoreProperties(ignoreUnknown=true)
public class GlobusGroup extends GroupsEntity
{
    @JsonProperty("invited_count")
    public int invitedCount;

    @JsonProperty("allow_invite")
    public boolean allowInvite;

    @JsonProperty("allow_join")
    public boolean allowJoin;

    @JsonProperty("id")
    public String id;

    @JsonProperty("description")
    public String description;

    @JsonProperty("user_status")
    public String userStatus;

    @JsonProperty("web_hook_url")
    public String webHookUrl;

    @JsonProperty("active_count")
    int activeCount;

    @JsonProperty("has_subgroups")
    public boolean hasSubgroups;

    @JsonProperty("group_type")
    public String groupType;

    @JsonProperty("invite")
    @JsonDeserialize(using=FromChildDeserializer.class)
    @JsonChildPath("url")
    public String invite;

    @JsonProperty("parent")
    public String parent;

    @JsonProperty("member_limit_count")
    int memberLimitCount;

    @JsonProperty("member_limit")
    int memberLimit;

    @JsonProperty("pending_count")
    int pendingCount;

    @JsonProperty("user_role")
    public MembershipRole userRole;

    @JsonProperty("allow_create_subgroups")
    public boolean allowCreateSubgroups;

    @JsonProperty("is_admin")
    public boolean isAdmin;

    @JsonProperty("members")
    @JsonDeserialize(using=FromChildDeserializer.class)
    @JsonChildPath("url")
    public String members;

    static class ClassForpath
    {
        @JsonProperty("id")
        public String id;

        @JsonProperty("name")
        public String name;

    }
    @JsonProperty("path")
    ClassForpath[] path;
    // Object path;

    @JsonProperty("join")
    @JsonDeserialize(using=FromChildDeserializer.class)
    @JsonChildPath("url")
    public String join;

    @JsonProperty("name")
    public String name;

    @JsonProperty("email_templates")
    @JsonDeserialize(using=FromChildDeserializer.class)
    @JsonChildPath("url")
    public String emailTemplates;

    @JsonProperty("messages")
    @JsonDeserialize(using=FromChildDeserializer.class)
    @JsonChildPath("url")
    public String messages;

    @JsonProperty("policies")
    @JsonDeserialize(using=FromChildDeserializer.class)
    @JsonChildPath("url")
    public String policies;

    @JsonProperty("web_hook_retry_delay")
    public int webHookRetryDelay;

    @JsonProperty("web_hook_retry_max_times")
    public int webHookRetryMaxTimes;

    @JsonIgnore
    public GroupMember[] getMembers() throws GlobusClientException
    {
        if (members == null || intfImpl == null) {
            return null;
        }
        GroupMemberList memberList = intfImpl.doGet(RequestType.groups, members, 200, GroupMemberList.class);
        if (memberList != null) {
            return memberList.members;
        } else {
            return null;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /*
    @Override
    public String toString()
    {
        final int maxLen = 3;
        return "GlobusGroup [invitedCount=" + invitedCount + ", allowInvite=" + allowInvite
            + ", allowJoin=" + allowJoin + ", " + (id != null ? "id=" + id + ", " : "")
            + (description != null ? "description=" + description + ", " : "")
            + (userStatus != null ? "userStatus=" + userStatus + ", " : "")
            + (webHookUrl != null ? "webHookUrl=" + webHookUrl + ", " : "") + "activeCount="
            + activeCount + ", hasSubgroups=" + hasSubgroups + ", "
            + (groupType != null ? "groupType=" + groupType + ", " : "")
            + (invite != null ? "invite=" + invite + ", " : "")
            + (parent != null ? "parent=" + parent + ", " : "") + "memberLimitCount="
            + memberLimitCount + ", memberLimit=" + memberLimit + ", pendingCount=" + pendingCount
            + ", " + (userRole != null ? "userRole=" + userRole + ", " : "")
            + "allowCreateSubgroups=" + allowCreateSubgroups + ", isAdmin=" + isAdmin + ", "
            + (members != null ? "members=" + members + ", " : "")
            + (path != null
                ? "path=" + Arrays.asList(path).subList(0, Math.min(path.length, maxLen)) + ", "
                : "")
            + (join != null ? "join=" + join + ", " : "")
            + (name != null ? "name=" + name + ", " : "")
            + (emailTemplates != null ? "emailTemplates=" + emailTemplates + ", " : "")
            + (messages != null ? "messages=" + messages + ", " : "")
            + (policies != null ? "policies=" + policies + ", " : "") + "webHookRetryDelay="
            + webHookRetryDelay + ", webHookRetryMaxTimes=" + webHookRetryMaxTimes + "]";
    }
    */    
}
