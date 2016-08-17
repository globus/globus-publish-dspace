/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 18, 2014 by pruyne
 */

package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class TaskList extends TransferEntity
{
    @JsonProperty("length")
    public Integer length;

    @JsonProperty("limit")
    public Integer limit;

    @JsonProperty("offset")
    public Integer offset;

    @JsonProperty("total")
    public Integer total;

    @JsonProperty("DATA")
    public Task[] data;
}
