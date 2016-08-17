/**
 * Copyright 2015 University of Chicago
 * All rights reserved.
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
public class RenameOperation
{
    @JsonProperty("DATA_TYPE")
    public String operationName;
    @JsonProperty("old_path")
    public String oldPath;
    @JsonProperty("new_path")
    public String newPath;


    public RenameOperation()
    {
        operationName = "rename";
    }
    
    public RenameOperation(String srcPath, String destPath)
    {
        this();
        this.oldPath = srcPath;
        this.newPath = destPath;
    }
}
