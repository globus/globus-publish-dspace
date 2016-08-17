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
public class TransferItem
{

    @JsonProperty("DATA_TYPE")
    String dataType = "transfer_item";

    @JsonProperty("source_endpoint")
    public String srcEndpoint;
    @JsonProperty("source_path")
    public String srcPath;
    @JsonProperty("destination_endpoint")
    public String destEndpoint;
    @JsonProperty("destination_path")
    public String destPath;
    @JsonProperty("recursive")
    public Boolean recursive;
    @JsonProperty("verify_size")
    public Long verifySize;


    public TransferItem()
    {

    }


    public TransferItem(String srcPath, String destPath)
    {
        this();
        this.srcPath = srcPath;
        this.destPath = destPath;
    }


    public TransferItem(String srcPath, String destPath, boolean recursive)
    {
        this(srcPath, destPath);
        this.recursive = Boolean.valueOf(recursive);
    }


    public TransferItem(String srcEndpoint, String srcPath, String destEndpoint, String destPath)

    {
        this(srcPath, destPath);
        this.srcEndpoint = srcEndpoint;
        this.destEndpoint = destEndpoint;
    }


    public TransferItem(String srcEndpoint, String srcPath, String destEndpoint, String destPath,
                        boolean recursive)
    {
        this(srcEndpoint, srcPath, destEndpoint, destPath);
        this.recursive = Boolean.valueOf(recursive);
    }


    public TransferItem(String srcEndpoint, String srcPath, String destEndpoint, String destPath,
                        boolean recursive, long verifySize)
    {
        this(srcEndpoint, srcPath, destEndpoint, destPath, recursive);
        this.verifySize = Long.valueOf(verifySize);
    }
}
