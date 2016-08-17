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
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.globus.GlobusEntity;
import org.globus.groups.GlobusGroupMembership;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GlobusUser extends GlobusEntity
{
    @JsonProperty("username")
    public String username;

    @JsonProperty("user_name")
    public String primaryUserName;
    
    @JsonProperty("email_validated")
    public boolean emailValidated;

    @JsonProperty("identity_id")
    public String identityId;
    
    @JsonProperty("ssh_pubkeys")
    public String[] sshPubkeys;

    @JsonProperty("opt_in")
    public Boolean optIn;

    @JsonProperty("custom_fields")
    public Map<String, Object> customFields;

    @JsonProperty("nonprofit")
    public String nonprofit;
    
    @JsonProperty("organization")
    public String organization;
    
    @JsonProperty("fullname")
    public String fullname;

    @JsonProperty("email")
    public String email;

    @JsonProperty("resource_type")
    public String resourceType;

    @JsonProperty("system_admin")
    public boolean systemAdmin;

    @JsonProperty("is_group_admin")
    public boolean groupAdmin;
    
    @JsonProperty("groups")
    public GlobusGroupMembership[] groups;

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /*
    @Override
    public String toString()
    {
        final int maxLen = 3;
        return "GlobusUser [" + (username != null ? "username=" + username + ", " : "")
            + (primaryUserName != null ? "primaryUserName=" + primaryUserName + ", "
                : "")
            + "emailValidated=" + emailValidated + ", "
            + (identityId != null ? "identityId=" + identityId + ", " : "")
            + (sshPubkeys != null ? "sshPubkeys="
                + Arrays.asList(sshPubkeys).subList(0, Math.min(sshPubkeys.length, maxLen)) + ", "
                : "")
            + (optIn != null ? "optIn=" + optIn + ", " : "")
            + (customFields != null
                ? "customFields=" + toString(customFields.entrySet(), maxLen) + ", " : "")
            + (nonprofit != null ? "nonprofit=" + nonprofit + ", " : "")
            + (organization != null ? "organization=" + organization + ", " : "")
            + (fullname != null ? "fullname=" + fullname + ", " : "")
            + (email != null ? "email=" + email + ", " : "")
            + (resourceType != null ? "resourceType=" + resourceType + ", " : "") + "systemAdmin="
            + systemAdmin + ", groupAdmin=" + groupAdmin + ", "
            + (groups != null
                ? "groups=" + Arrays.asList(groups).subList(0, Math.min(groups.length, maxLen))
                : "")
            + "]";
    }
    */

    private String toString(Collection<?> collection, int maxLen)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }

}
