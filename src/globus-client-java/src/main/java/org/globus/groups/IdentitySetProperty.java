/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Dec 24, 2015 by pruyne
 */

package org.globus.groups;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.globus.groups.GroupsInterface.MembershipRole;
import org.globus.groups.GroupsInterface.MembershipStatus;

/**
 * @author  pruyne
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class IdentitySetProperty
{
    /**
     * 
     */
    @JsonProperty("allow_join")
    public Boolean allowJoin;
    /**
     * 
     */
    @JsonProperty("role")
    public MembershipRole role;
    /**
     * 
     */
    @JsonProperty("allow_view_members")
    public Boolean allowViewMembers;
    /**
     * 
     */
    @JsonProperty("allow_create_subgroups")
    public Boolean allowCreateSubgroups;
    /**
     * 
     */
    @JsonProperty("allow_invite")
    public Boolean allowInvite;
    /**
     * 
     */
    @JsonProperty("status")
    public MembershipStatus status;


    /**
     * 
     */
    public IdentitySetProperty()
    {
    }
}