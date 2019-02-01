/*
 * Copyright 2019 University of Chicago
 * All rights reserved.
 */

package org.globus.identifiers;

import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface;

public interface IdentifiersInterface extends GlobusRestInterface
{
    /*
    public Namespace createNamespace(Namespace ns) throws GlobusClientException;
    public Namespace updateNamespace(Namespace ns) throws GlobusClientException;
    public Namespace getNamespace(String id) throws GlobusClientException;
    public boolean deleteNamespace(String id) throws GlobusClientException;

    public Identifier updateIdentifier(Identifier identifier)
        throws GlobusClientException;
    */
	/**
	 * 
	 * @param namespaceId
	 * @param identifier
	 * @return
	 * @throws GlobusClientException
	 */
    public Identifier createIdentifier(String namespaceId,
                                       Identifier identifier)
        throws GlobusClientException;

    /**
     * 
     * @param namespaceId
     * @param identifier
     * @param prefix
     * @return
     * @throws GlobusClientException
     */
    public Identifier createIdentifier(String namespaceId, Identifier identifier, String prefix)
    		throws GlobusClientException;
    
    /**
     * 
     * @param id
     * @return
     * @throws GlobusClientException
     */
    public Identifier getIdentifier(String id) throws GlobusClientException;
}
