/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 15, 2014 by pruyne
 */

package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.globus.transfer.TransferInterface.PrincipalType;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class TransferAccess extends TransferEntity
{
    @JsonProperty("DATA_TYPE")
    public String dataType;

    @JsonProperty("principal_type")
    public PrincipalType principalType;

    @JsonProperty("path")
    public String path;

    @JsonProperty("principal")
    public String principal;

    @JsonProperty("id")
    public String id;

    @JsonProperty("permissions")
    public String permissions;

    @JsonProperty("role_id")
    public String roleId;
}
