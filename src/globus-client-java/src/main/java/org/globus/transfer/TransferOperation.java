/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 */

package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.globus.transfer.TransferInterface.PrincipalType;

/**
 * @author pruyne
 *
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TransferOperation
{
    public enum OperationId {
        shared_endpoint,
        mkdir,
        access
    }

    @JsonProperty("DATA_TYPE")
    public OperationId operationName;
    @JsonProperty("name")
    public String name;
    @JsonProperty("path")
    public String path;
    @JsonProperty("host_endpoint")
    public String hostEndpoint;
    @JsonProperty("host_path")
    public String hostPath;
    @JsonProperty("description")
    public String description;
    @JsonProperty("editable")
    public Boolean editable;
    @JsonProperty("principal_type")
    public PrincipalType principalType;
    @JsonProperty("principal")
    public String principal;
    @JsonProperty("permissions")
    public String permissions;
    @JsonProperty("send_email")
    public Boolean sendEmail;


    public TransferOperation()
    {

    }


    public TransferOperation(OperationId opId)
    {
        operationName = opId;
    }


    /**
     * TODO:
     * @param directoryName
     * @return
     */
    public static TransferOperation mkdir(String directoryName)
    {
        TransferOperation op = new TransferOperation(OperationId.mkdir);
        op.path = directoryName;
        return op;
    }


    /**
     * @param endpoint
     * @param sharePath
     * @param sharingName
     * @param sharingComment
     * @return
     */
    public static TransferOperation sharedEndpoint(String endpoint, String sharePath,
                                                   String sharingName, String sharingComment)
    {
        TransferOperation op = new TransferOperation(OperationId.shared_endpoint);
        op.description = sharingComment;
        op.name = sharingName;
        op.hostEndpoint = endpoint;
        op.hostPath = sharePath;
        return op;
    }


    /**
     * TODO:
     * @param path
     * @param type
     * @param principalName
     * @param permissions
     * @param editable
     * @return
     */
    public static TransferOperation access(String path, PrincipalType type, String principalName,
                                           String permissions)
    {
        TransferOperation top = new TransferOperation(OperationId.access);

        top.path = path;
        top.principalType = type;
        top.principal = principalName;
        top.permissions = permissions;

        return top;
    }
}
