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

public class IdType extends GlobusEntity {
    @JsonChildPath("data/id")
    public String id;

    @JsonChildPath("data/type")
    public String dataType;
    
    @JsonProperty("data")
    private void setData(JsonNode node) {
        setChildren("data", node);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "IdType [" + (id != null ? "id=" + id + ", " : "")
            + (dataType != null ? "dataType=" + dataType : "") + "]";
    }    
}