/*
 * Copyright 2019 University of Chicago
 *
 * All Rights Reserved.
 */

/* 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.globus.identifiers;

import java.io.IOException;

import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.auth.GlobusAuthToken;

public class IdentifiersClient extends GlobusClient implements IdentifiersInterface
{
    private static final String DEFAULT_SERVICE_URL = "https://identifiers.globus.org";
    // private static final String DEFAULT_SERVICE_URL = "http://localhost:5000";

    public IdentifiersClient() {
        super();
    }

    public IdentifiersClient(GlobusAuthToken token, String identifiersServiceUrl) {
        super(token);
        if (identifiersServiceUrl == null) {
            identifiersServiceUrl = DEFAULT_SERVICE_URL;
        }
        setRootUrlForRequestType(RequestType.identifiers, identifiersServiceUrl);
    }
    
    public IdentifiersClient(String accessTokenJson) throws IOException {
        super(accessTokenJson);
    }

    public IdentifiersClient(GlobusAuthToken authToken) {
        this(authToken, null);
    }

    /**
       Build an Identifiers-specific client based on authorization and other
       info cached in another GlobusClient.
     */
    public IdentifiersClient(GlobusClient client) {
        // TODO: How do we cache auth tokens for different scopes in the Auth
        // token object
        super(client.getAuthTokenForRequestType(null));
    }
    
    public Identifier createIdentifier(String namespaceId,
                                       Identifier identifier) throws GlobusClientException {
    	return createIdentifier(namespaceId, identifier, null);
    }

	/* (non-Javadoc)
	 * @see org.globus.identifiers.IdentifiersInterface#createIdentifier(java.lang.String, org.globus.identifiers.Identifier, java.lang.String)
	 */
	@Override
	public Identifier createIdentifier(String namespaceId, Identifier identifier, String prefix)
			throws GlobusClientException {
    	String url = "/namespace/"+namespaceId+"/identifier";
    	if (prefix != null) {
    		url = url + "/" + prefix;
    	}
		Identifier retIdentifier = this.doPost(RequestType.identifiers,
                url,
                201, null, identifier,
                Identifier.class);
    	return retIdentifier;
	}
	
    public Identifier getIdentifier(String id) throws GlobusClientException 
    {
        Identifier identifier = this.doGet(RequestType.atmosphere, id, 200,
                                           null, Identifier.class);
        return identifier;
    }

}
