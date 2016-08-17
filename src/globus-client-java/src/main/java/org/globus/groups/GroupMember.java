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

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.globus.FromChildDeserializer;
import org.globus.GlobusClientException;
import org.globus.GlobusEntity;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.JsonChildPath;
import org.globus.auth.GlobusUser;
import org.globus.groups.GroupsInterface.MembershipRole;
import org.globus.groups.GroupsInterface.MembershipStatus;

/**
 * @author pruyne
 *
 */

/*
 * The amount of information returned per user varies depending on whether a specific username is
 * given on the query, so we make the fields ignorable so one structure can be used in either case.
 */

// @JsonIgnoreProperties(ignoreUnknown = true)
public class GroupMember extends GlobusEntity
{
    @JsonProperty("username")
    public String username;

    @JsonProperty("status")
    public MembershipStatus status;

    static class ClassForinvitedBy
    {
        @JsonProperty("username")
        public String username;

        @JsonProperty("fullname")
        public String fullname;


        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("ClassForinvitedBy [");
            if (username != null) {
                builder.append("username=").append(username).append(", ");
            }
            if (fullname != null) {
                builder.append("fullname=").append(fullname);
            }
            builder.append("]");
            return builder.toString();
        }

    }

    @JsonChildPath("invited_by/username")
    public String invitedByUsername;

    @JsonChildPath("invited_by/fullname")
    public String invitedByFullname;


    @JsonProperty("invited_by")
    private void setInvitedBy(JsonNode node)
    {
        setChildren("invited_by", node);
    }

    // public ClassForinvitedBy invitedBy;

    static class ClassForInfo
    {
        @JsonProperty("url")
        public String url;
    }

    @JsonProperty("info")
    @JsonDeserialize(using = FromChildDeserializer.class)
    public String info;

    @JsonProperty("role")
    public MembershipRole role;

    @JsonProperty("identity_id")
    public String identityId;

    @JsonProperty("name")
    public String name;

    @JsonProperty("invite_time")
    public String inviteTime;

    @JsonProperty("last_changed")
    public String lastChanged;

    @JsonProperty("status_reason")
    public String statusReason;

    @JsonProperty("email")
    public String email;

    static class ClassForuser
    {
        @JsonProperty("city")
        public String city;

        @JsonProperty("first_name")
        public String firstName;

        @JsonProperty("last_name")
        public String lastName;

        @JsonProperty("current_project_name")
        public String currentProjectName;

        @JsonProperty("address1")
        public String address1;

        @JsonProperty("url")
        public String url;

        @JsonProperty("country")
        public String country;

        @JsonProperty("address2")
        public String address2;

        @JsonProperty("field_of_science")
        public String fieldOfScience;

        @JsonProperty("phone")
        public String phone;

        @JsonProperty("state")
        public String state;

        @JsonProperty("zip")
        public String zip;

        @JsonProperty("address")
        public String address;

        @JsonProperty("department")
        public String department;

        @JsonProperty("organization")
        public String organization;

        @JsonProperty("institution")
        public String institution;


        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("ClassForuser [");
            if (city != null) {
                builder.append("city=").append(city).append(", ");
            }
            if (firstName != null) {
                builder.append("firstName=").append(firstName).append(", ");
            }
            if (lastName != null) {
                builder.append("lastName=").append(lastName).append(", ");
            }
            if (currentProjectName != null) {
                builder.append("currentProjectName=").append(currentProjectName).append(", ");
            }
            if (address1 != null) {
                builder.append("address1=").append(address1).append(", ");
            }
            if (url != null) {
                builder.append("url=").append(url).append(", ");
            }
            if (country != null) {
                builder.append("country=").append(country).append(", ");
            }
            if (address2 != null) {
                builder.append("address2=").append(address2).append(", ");
            }
            if (fieldOfScience != null) {
                builder.append("fieldOfScience=").append(fieldOfScience).append(", ");
            }
            if (phone != null) {
                builder.append("phone=").append(phone).append(", ");
            }
            if (state != null) {
                builder.append("state=").append(state).append(", ");
            }
            if (zip != null) {
                builder.append("zip=").append(zip).append(", ");
            }
            if (address != null) {
                builder.append("address=").append(address).append(", ");
            }
            if (department != null) {
                builder.append("department=").append(department).append(", ");
            }
            if (organization != null) {
                builder.append("organization=").append(organization).append(", ");
            }
            if (institution != null) {
                builder.append("institution=").append(institution);
            }
            builder.append("]");
            return builder.toString();
        }

    }
    @JsonProperty("user")
    public ClassForuser user;


    @JsonIgnore
    public GlobusUser getUserDetails() throws GlobusClientException
    {
        if (info != null) {
            GlobusUser user = super.getIntfImpl().doGet(RequestType.groups, info + "/details", 200,
                                                        GlobusUser.class);
            return user;
        } else {
            return null;
        }
    }

}
