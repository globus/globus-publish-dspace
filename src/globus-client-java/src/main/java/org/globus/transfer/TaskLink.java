/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 18, 2014 by pruyne
 */

package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class TaskLink extends TransferEntity
{
    @JsonProperty("href")
    public String href;

    @JsonProperty("resource")
    public String resource;

    @JsonProperty("rel")
    public String rel;

    @JsonProperty("title")
    public String title;

}