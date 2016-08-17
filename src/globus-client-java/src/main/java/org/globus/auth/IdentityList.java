/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Jan 4, 2016 by pruyne
 */

package org.globus.auth;

import java.util.Arrays;

import org.globus.JsonChildPath;

/**
 * @author pruyne
 *
 */
public class IdentityList extends JsonAPIEntity
{
    @JsonChildPath("data/")
    public Identity[] identities;
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Arrays.asList(identities).toString();
    }
}
