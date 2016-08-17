/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 18, 2014 by pruyne
 */

package org.globus.transfer;

import java.util.Arrays;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class SuccessfulTransfers
{
    @JsonProperty("DATA_TYPE")
    public String dataType;

    @JsonProperty("marker")
    public Integer marker;

    @JsonProperty("next_marker")
    public Integer nextMarker;

    @JsonProperty("DATA")
    public SuccessfulTransfer[] data;


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SuccessfulTransfer [");
        if (dataType != null) {
            builder.append("dataType=").append(dataType).append(", ");
        }
        if (marker != null) {
            builder.append("marker=").append(marker).append(", ");
        }
        if (nextMarker != null) {
            builder.append("next_marker=").append(nextMarker).append(", ");
        }
        if (data != null) {
            builder.append("data=").append(Arrays.toString(data));
        }
        builder.append("]");
        return builder.toString();
    }

}
