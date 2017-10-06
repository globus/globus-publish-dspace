/** 
 * This file is a modified version of a DSpace file.  
 * All modifications are subject to the following copyright and license.
 *
 * Copyright 2014-2016 The University of Chicago
 * 
 * All rights reserved.
 */

package org.dspace.globus;

import org.apache.log4j.Logger;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.auth.GlobusAuthToken;
import org.json.JSONObject;

/**
 * @author pruyne
 *
 */
public class GlobusSearchClient extends GlobusClient
{
    private static final Logger log = Logger.getLogger(GlobusSearchClient.class);

    private String indexName;
    
    public GlobusSearchClient(GlobusAuthToken token, String searchServiceUrl,
            String indexName) 
    {
        super(token);
        setRootUrlForRequestType(RequestType.search, searchServiceUrl);
        this.indexName = indexName;
    }
    
    public boolean index(JSONObject content, String subject, String[] visibleTo)
    throws GlobusClientException
    {
        JSONObject gingest = new JSONObject();
        gingest.put("@datatype", "GIngest");
        gingest.put("@version", "2017-09-01");
        gingest.put("ingest_type", "GMetaEntry");

        if (visibleTo == null || visibleTo.length == 0) {
            visibleTo = new String[] {"public"};
        }
        JSONObject gmeta = new JSONObject();
        gmeta.put("@datatype", "GMetaEntry");
        gmeta.put("@version", "2017-09-01");
        gmeta.put("subject", subject);
        gmeta.put("visible_to", visibleTo);
        gmeta.put("mimetype", "application/json");
        gmeta.put("content", content);
        
        gingest.put("ingest_data", gmeta);

        log.info("Indexing entry: " + gingest);
        String ingestUrl = "/index/" + indexName + "/ingest";
        Object ingestReply = doPost(RequestType.search, ingestUrl, 200, 
                null, gingest.toString(), Object.class);
        log.info("Index of " + subject + " returned " + ingestReply);
        return ingestReply != null;
    }
}
