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

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author pruyne
 *
 */
public class GlobusGroupMembership
{
    @JsonProperty("status")
    public String status;

    @JsonProperty("membership_status")
    public GroupsInterface.MembershipStatus membershipStatus;

    @JsonProperty("description")
    public String description;

    @JsonProperty("membership_role")
    public GroupsInterface.MembershipRole membershipRole;

    @JsonProperty("id")
    public String id;

    @JsonProperty("group_type")
    public String groupType;

    @JsonProperty("name")
    public String name;


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "GlobusGroupMembership [status=" + status + ", membershipStatus=" + membershipStatus
            + ", description=" + description + ", membershipRole=" + membershipRole + ", id=" + id
            + ", groupType=" + groupType + ", name=" + name + "]";
    }

}