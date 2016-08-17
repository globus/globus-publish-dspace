/**
 * Copyright 2014 University of Chicago
 * All Rights Reserved.
 */

package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * @author pruyne
 *
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SuccessfulTransfer
{

    
	@JsonProperty("DATA_TYPE")
	public String dataType;
	 
    @JsonProperty("source_path")
    public String srcPath;
   
    @JsonProperty("destination_path")
    public String destPath;
    


    public SuccessfulTransfer()
    {

    }


    public SuccessfulTransfer(String srcPath, String destPath)
    {
        this();
        this.srcPath = srcPath;
        this.destPath = destPath;
    }


}
