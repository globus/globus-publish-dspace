/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Jan 4, 2016 by pruyne
 */

package org.globus.auth;

import java.io.IOException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.globus.GlobusEntity;
import org.globus.JsonChildPath;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Identity extends JsonAPIEntity
{
    @JsonChildPath("data/attributes/username")
    public String username;

    @JsonChildPath("data/attributes/email")
    public String email;

    @JsonChildPath("data/attributes/name")
    public String name;

    @JsonChildPath("data/attributes/organization")
    public String organization;

    @JsonChildPath("data/attributes/status")
    public String status;
    
    @JsonProperty("identity_provider")
    public String idProvider; 


//    @JsonChildPath("data/relationships/identity_provider")
//    public IdType idProvider;
    
    @JsonChildPath("data/relationships/linked_identities")
    public IdType[] linkedIds;
    
    public static GlobusEntity fromJson(String json) throws IOException
    {
        return fromJson(json, Identity.class);
    }
    
}
