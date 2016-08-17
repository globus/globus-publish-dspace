/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Jan 5, 2016 by pruyne
 */

package org.globus.auth;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.globus.GlobusEntity;
import org.globus.JsonChildPath;

/**
 * @author pruyne
 *
 */
public class JsonAPIEntity extends GlobusEntity
{

    @JsonChildPath("data/id")
    public String id;
    @JsonChildPath("data/type")
    public String type;


    @JsonProperty("data")
    private void setData(JsonNode node)
    {
        setChildren("data", node);
    }


    @JsonProperty("attributes")
    private void setAttributes(JsonNode node)
        throws IllegalArgumentException, IllegalAccessException
    {
        setChildren("attributes", node);
    }


    @JsonProperty("relationships")
    private void setRelationships(JsonNode node)
    {
        setChildren("relationships", node);
    }


    @JsonProperty("included")
    private void setIncluded(JsonNode node)
    {
        setChildren("included", node);
    }
}
