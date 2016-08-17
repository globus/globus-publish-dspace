/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 15, 2014 by pruyne
 */

package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class TransferAccessList extends TransferEntity
{
    @JsonProperty("DATA_TYPE")
    public String dataType;

    @JsonProperty("DATA")
    public TransferAccess[] accessItems;

    @JsonProperty("endpoint")
    public String endpoint;
}
