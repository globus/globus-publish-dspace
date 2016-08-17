/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Jan 6, 2016 by pruyne
 */

package org.globus.auth;

import org.globus.JsonChildPath;

/**
 * @author pruyne
 *
 */
public class IdentityProvider extends JsonAPIEntity
{
    @JsonChildPath("alternatives/name")
    public String name;
    
    @JsonChildPath("alternatives/category")
    public String category;
    
    @JsonChildPath("attributes/alternative_names")
    public String[] alternativeNames;
    
    @JsonChildPath("attributes/namespaces")
    public String[] namespaces;
}
