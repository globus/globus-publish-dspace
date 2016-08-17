/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Dec 24, 2015 by pruyne
 */

package org.globus.groups;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.globus.GlobusEntity;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GroupsEntity extends GlobusEntity
{
    @JsonProperty("identity_set_properties")
    public Map<String, Object> identitySetProperties;

    public GroupsInterface getIntfImpl()
    {
        if (super.getIntfImpl() instanceof GroupsInterface) {
            return (GroupsInterface) super.getIntfImpl();
        } else {
            return null;
        }
    }

}
