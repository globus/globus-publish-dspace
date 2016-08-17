package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Created by pruyne on 4/21/15.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class DeleteTask
{
    /*
    {
  "submission_id": "cd19f0d2-e854-11e4-b6b2-1231392cc9a8",
  "endpoint": "go#ep2",
  "recursive": false,
  "DATA_TYPE": "delete",
  "interpret_globs": false,
  "label": "example transfer label",
  "length": 2,
  "deadline": "2015-04-22 18:32:44+00:00",
  "notify_on_succeeded": true,
  "ignore_missing": false,
  "notify_on_failed": true,
  "DATA": [
    {
      "path": "/~/bashrc_copy_example",
      "DATA_TYPE": "delete_item"
    }
  ],
  "notify_on_inactive": true
}
     */

    @JsonProperty("DATA_TYPE")
    public String dataType = "delete";
    
    @JsonProperty("submission_id")
    public String submissionId;

    @JsonProperty("endpoint")
    public String endpoint;

    @JsonProperty("recursive")
    public Boolean recursive;

    @JsonProperty("notify_on_failed")
    public Boolean notifyOnFailure;
    
    @JsonProperty("notify_on_succeeded")
    public Boolean notifyOnSuccess;

    @JsonProperty("ignore_missing")
    public Boolean ignoreMissing;
    
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private class DeleteData
    {
        @JsonProperty("DATA_TYPE")
        public String dataType = "delete_item";
        @JsonProperty("path")
        public String path;
    }

    @JsonProperty("DATA")
    DeleteData[] deleteItems;

    public DeleteTask(String submissionId, String endpoint, String path, boolean recursive)
    {
        DeleteData dd = new DeleteData();
        dd.path = path;
        this.submissionId = submissionId;
        this.endpoint = endpoint;
        this.recursive = recursive;
        this.deleteItems = new DeleteData[] { dd };
    }

    public DeleteTask(String submissionId, String endpoint, String path)
    {
        this(submissionId, endpoint, path, false);
    }
}
