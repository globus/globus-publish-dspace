/**
 * Copyright 2014 University of Chicago.
 * All Rights Reserved.
 */

package org.globus.transfer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface.RequestType;

/**
 * @author pruyne
 *
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TransferJob 
{
    public enum SyncLevel {
        NOT_EXIST(0),
        SIZE_DIFF(1),
        TIME_DIFF(2),
        CHECKSUM_DIFF(3);

        private int syncLevel;


        private SyncLevel(int level)
        {
            syncLevel = level;
        }


        @JsonValue
        public int getLevel()
        {
            return syncLevel;
        }
    };

    @JsonProperty("DATA_TYPE")
    String transferDataType = "transfer";

    @JsonProperty("length")
    public int length;

    @JsonProperty("submission_id")
    public String submissionId;

    @JsonProperty("label")
    public String label;

    @JsonProperty("notify_on_succeeded")
    public Boolean notifyOnSucceeded;

    @JsonProperty("notify_on_failed")
    public Boolean notifyOnFailed;

    @JsonProperty("notify_on_inactive")
    public Boolean notifyOnInactive;

    @JsonProperty("deadline")
    public Date deadline;

    @JsonProperty("sync_level")
    public SyncLevel syncLevel;

    @JsonProperty("encrypt_data")
    public Boolean encryptData;

    @JsonProperty("verify_checksum")
    public Boolean verifyChecksum;

    @JsonProperty("delete_destination_extra")
    public Boolean deleteDestinationExtra;

    @JsonProperty("preserve_timestamp")
    public Boolean preserveTimestamp;

    @JsonProperty("source_endpoint")
    public String sourceEndpoint;

    @JsonProperty("destination_endpoint")
    public String destinationEndpoint;

    @JsonProperty("DATA")
    List<TransferItem> transferItems;

    private final TransferInterface client;


    public TransferJob(TransferInterface client)
    {
        this.client = client;
        transferItems = new ArrayList<>();
    }


    public TransferJob(TransferInterface client, String srcEndpoint, String destEndpoint)

    {
        this(client);
        sourceEndpoint = srcEndpoint;
        destinationEndpoint = destEndpoint;
    }


    public void addTransferItem(TransferItem item)
    {
        transferItems.add(item);
        length = transferItems.size();
    }


    public void addTransferItem(String srcPath, String destPath, boolean recursive)
    {
        TransferItem ti = new TransferItem(srcPath, destPath, recursive);
        addTransferItem(ti);
    }


    /**
     * Execute the TransferJob described by this structure. Note that multiple calls to this method
     * will result in multiple executions of the same transfer job. Further, if a TransferJob has
     * been initiated, then parameters changed (such as new {@link TransferItem}s added or a change
     * in endpoints, etc.), it may be re-executed to begin a new transfer. However, for the duration
     * of this call to initiate the transfer, the Transfer object is locked because state of the
     * TransferJob is indeterminate during the creation of the Transfer request.
     * @return A {@link TaskOld} object which can be queried to monitor the state of the transfer.
     */
    public synchronized Task execute() throws GlobusClientException
    {
        submissionId = client.createSubmissionId();
        Task task = client.doPost(RequestType.transfer, "/transfer", 202, null, this, Task.class);

        return task;
    }
}
