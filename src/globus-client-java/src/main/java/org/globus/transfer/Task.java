/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 18, 2014 by pruyne
 */

package org.globus.transfer;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.globus.FromChildDeserializer;
import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.JsonChildPath;
import org.globus.jsonUtil.GlobusJsonDateDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Task extends TransferEntity
{

    public enum Status {
        ACTIVE,
        INACTIVE,
        SUCCEEDED,
        FAILED;
    }

    @JsonProperty("task_id")
    public String taskId;

    @JsonProperty("type")
    public String type;

    @JsonProperty("status")
    public Status status;

    @JsonProperty("label")
    public Object label;

    @JsonProperty("username")
    public String username;

    @JsonProperty("request_time")
    @JsonDeserialize(using = GlobusJsonDateDeserializer.class)
    public Date requestTime;

    @JsonProperty("completion_time")
    @JsonDeserialize(using = GlobusJsonDateDeserializer.class)
    public Date completionTime;

    @JsonProperty("deadline")
    public String deadline;

    @JsonProperty("source_endpoint")
    public String sourceEndpoint;

    @JsonProperty("source_endpoint_id")
    public String sourceEndpointId;

    @JsonProperty("source_endpoint_display_name")
    public String sourceEndpointDisplayName;

    @JsonProperty("destination_endpoint")
    public String destinationEndpoint;

    @JsonProperty("destination_endpoint_id")
    public String destinationEndpointId;

    @JsonProperty("destination_endpoint_display_name")
    public String destinationEndpointDisplayName;

    @JsonProperty("faults")
    public Integer faults;

    @JsonProperty("subtasks_retrying")
    public Integer subtasksRetrying;

    @JsonProperty("effective_bytes_per_second")
    public Long effectiveBytesPerSecond;

    @JsonProperty("nice_status")
    public String niceStatus;

    @JsonProperty("canceled_by_admin")
    public String canceldByAdmin;

    @JsonProperty("canceled_by_admin_message")
    public String canceldByAdminMessage;

    @JsonProperty("is_paused")
    public Boolean isPaused;

    @JsonProperty("bytes_transferred")
    public Long bytesTransferred;

    @JsonProperty("sync_level")
    public Object syncLevel;

    @JsonProperty("files")
    public Integer files;

    @JsonProperty("delete_destination_extra")
    public Boolean deleteDestinationExtra;

    @JsonProperty("nice_status_details")
    public Object niceStatusDetails;

    @JsonProperty("subtasks_expired")
    public Integer subtasksExpired;

    @JsonProperty("subtasks_canceled")
    public Integer subtasksCanceled;

    @JsonProperty("subtasks_total")
    public Integer subtasksTotal;

    @JsonProperty("nice_status_expires_in")
    public Object niceStatusExpiresIn;

    @JsonProperty("subtask_link")
    public TaskLink subtaskLink;

    @JsonProperty("bytes_checksummed")
    public Long bytesChecksummed;

    @JsonProperty("subtasks_failed")
    public Integer subtasksFailed;

    @JsonProperty("history_deleted")
    public Boolean historyDeleted;

    @JsonProperty("files_skipped")
    public Integer filesSkipped;

    @JsonProperty("files_transferred")
    public Integer filesTransferred;

    @JsonProperty("nice_status_short_description")
    public String niceStatusShortDescription;

    @JsonProperty("preserve_timestamp")
    public Boolean preserveTimestamp;

    @JsonProperty("event_link")
    public TaskLink eventLink;

    @JsonProperty("encrypt_data")
    public Boolean encryptData;

    @JsonProperty("subtasks_succeeded")
    public Integer subtasksSucceeded;

    @JsonProperty("command")
    public String command;

    @JsonProperty("subtasks_pending")
    public Integer subtasksPending;

    @JsonProperty("verify_checksum")
    public Boolean verifyChecksum;

    @JsonProperty("directories")
    public Integer directories;




    /////// Begin not documented, added based on tests
    @JsonProperty("submission_id")
    public String submissionId;

    @JsonProperty("code")
    public String code;

    @JsonProperty("resource")
    public String resource;

    @JsonProperty("request_id")
    public String requestId;

    @JsonProperty("message")
    public String message;


    @JsonProperty("task_link")
    @JsonDeserialize(using=FromChildDeserializer.class)
    @JsonChildPath("href")
    public String taskLink;

    /// End not documented, and added


    private Status lastStatus;

    public Task()
    {

    }


    Task(TransferInterface client)
    {
        this();
        this.intfImpl = client;
    }


    Task(TransferInterface client, String id)
    {
        this(client);
        taskId = id;
    }


    public Status pollStatus() throws GlobusClientException
    {
        if (taskId == null) {
            return null;
        }
        String resource = "/task/" + taskId;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("fields", "status");
        @SuppressWarnings("unchecked")
        Map<String, Object> status =
            intfImpl.doGet(RequestType.transfer, resource, 200, params, Map.class);
        if (status != null) {
            if ("task".equals(status.get("DATA_TYPE"))) {
                Object statusObj = status.get("status");
                if (statusObj != null) {
                    String statusString = statusObj.toString();
                    lastStatus = Status.valueOf(statusString);
                    this.status = lastStatus;
                }
            }
        }
        return lastStatus;
    }

    public Status waitForStatus(long maxWaitTime, Status... goalStatus) throws GlobusClientException
    {
        if (goalStatus == null || goalStatus.length == 0) {
            return null;
        }
        List<Status> goalStatusList = Arrays.asList(goalStatus);
        if (status != null && goalStatusList.contains(status)) {
            return status;
        }

        long waitTime = 200;
        long totalWaitTime = 0;
        long pollTimeMax = 10000;
        do {
            Status pollStatus = pollStatus();
            if (pollStatus != null && goalStatusList.contains(pollStatus)) {
                return pollStatus;
            }
            try {
                Thread.sleep(waitTime);
                totalWaitTime += waitTime;
                // Do an exponential back-off on time we poll
                waitTime *= 2;
                if (waitTime > pollTimeMax) {
                    waitTime = pollTimeMax;
                }
            } catch (InterruptedException e) {
                throw new GlobusClientException("Error polling for delete task status " + e);
            }
        } while (totalWaitTime < maxWaitTime);

        return null;
    }


    public Object cancel() throws GlobusClientException
    {
        String path = "/task/" + taskId + "/cancel";
        Object cancelResult =
            intfImpl.doPost(RequestType.transfer, path, 202, null, null, Object.class);
        return cancelResult;
    }


    public Status getLastStatus()
    {
        return lastStatus;
    }


}
