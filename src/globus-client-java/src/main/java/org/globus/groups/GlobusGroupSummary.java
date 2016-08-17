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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.globus.GlobusClientException;
import org.globus.groups.GroupsEntity;
import org.globus.groups.GroupsInterface;
import org.globus.groups.IdentitySetProperty;
import org.globus.groups.GroupsInterface.MembershipRole;
import org.globus.groups.GroupsInterface.MembershipStatus;

import java.util.Map;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GlobusGroupSummary extends GroupsEntity
{
    @JsonProperty("create_date")
    public String createDate;

    @JsonProperty("description")
    public String description;

    @JsonProperty("web_hook_url")
    public String webHookUrl;

    @JsonProperty("url")
    public String url;

    @JsonProperty("my_status")
    public MembershipStatus myStatus;

    @JsonProperty("members_path")
    public String membersPath;

    @JsonProperty("my_role")
    public MembershipRole myRole;

    @JsonProperty("name")
    public String name;

    @JsonProperty("has_subgroups")
    public boolean hasSubgroups;

    @JsonProperty("allow_create_subgroups")
    public boolean allowCreateSubgroups;

    @JsonProperty("group_type")
    public String groupType;

    @JsonProperty("web_hook_retry_delay")
    public int webHookRetryDelay;

    @JsonProperty("last_updated")
    public String lastUpdated;

    @JsonProperty("id")
    public String id;

    @JsonProperty("resource_type")
    public String resourceType;

    @JsonProperty("web_hook_retry_max_times")
    public int webHookRetryMaxTimes;

    @JsonProperty("identity_set_properties")
    public Map<String, IdentitySetProperty> identitySetProperties;

    public GlobusGroup getGroup() throws GlobusClientException
    {
        GroupsInterface intfImpl = getIntfImpl();
        if (intfImpl != null) {
            return intfImpl.getGroupById(id);
        } else {
            return null;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "GlobusGroupSummary ["
            + (createDate != null ? "createDate=" + createDate + ", " : "")
            + (description != null ? "description=" + description + ", " : "")
            + (webHookUrl != null ? "webHookUrl=" + webHookUrl + ", " : "")
            + (url != null ? "url=" + url + ", " : "")
            + (myStatus != null ? "myStatus=" + myStatus + ", " : "")
            + (membersPath != null ? "membersPath=" + membersPath + ", " : "")
            + (myRole != null ? "myRole=" + myRole + ", " : "")
            + (name != null ? "name=" + name + ", " : "") + "hasSubgroups=" + hasSubgroups
            + ", allowCreateSubgroups=" + allowCreateSubgroups + ", "
            + (groupType != null ? "groupType=" + groupType + ", " : "") + "webHookRetryDelay="
            + webHookRetryDelay + ", "
            + (lastUpdated != null ? "lastUpdated=" + lastUpdated + ", " : "")
            + (id != null ? "id=" + id + ", " : "")
            + (resourceType != null ? "resourceType=" + resourceType + ", " : "")
            + "webHookRetryMaxTimes=" + webHookRetryMaxTimes + ", " + (identitySetProperties != null
                ? "identitySetProperties=" + identitySetProperties : "")
            + "]";
    }

}
