/**
 * This file is a modified version of a DSpace file.
 * All modifications are subject to the following copyright and license.
 * 
 * Copyright 2017 University of Chicago. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dspace.discovery;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.globus.Globus;
import org.dspace.globus.GlobusDataSearchClient;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.auth.GlobusAuthToken;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class GlobusDataSearchServiceImpl extends SolrServiceImpl
    implements SearchService, IndexingService {

    private static final Logger log = Logger.getLogger(GlobusDataSearchServiceImpl.class);
    GlobusDataSearchClient searchClient;

    public GlobusDataSearchServiceImpl()
    {
        super();
        log.info("Started GlobusDataSearchServiceImpl");
    }

    private GlobusDataSearchClient getSearchClient(Context context)
    {
        GlobusClient gc;
        if (context == null) {
            gc = Globus.getPrivlegedClient();
        } else {
            gc = Globus.getGlobusClientFromContext(context).getClient();
        }
        String searchUrl = 
                Globus.getGlobusConfigProperty(Globus.CONFIG_GLOBUS_DATASEARCH_URL);
        GlobusAuthToken searchToken = gc.getAuthTokenForRequestType(RequestType.search);
        searchClient = new GlobusDataSearchClient(searchToken, searchUrl);
        return searchClient;
    }
    
    /**
     * @param dso
     * @return
     */
    private String subjectForItem(Item item)
    {
        return item.getUri();
    }

    private void getSubGroupsTransitive(Group g, Set<Group> groups)
    {
        if (!groups.contains(g)) {
            groups.add(g);
            for (Group sg : g.getMemberGroups()) {
                getSubGroupsTransitive(sg, groups);
            }
        }
    }
    /**
     * @param dso
     * @return
     */
    private String[] visibleToListForDSpaceObject(Context context,
            DSpaceObject dso)
    {
        try
        {
            Group[] authorizedGroups = 
                    AuthorizeManager.getAuthorizedGroups(context, dso, 
                                                         Constants.READ);
            ArrayList<String> visibleTo = new ArrayList<String>();
            if (authorizedGroups != null) {
                Set<Group> allGroups = new HashSet<Group>();
                for (Group group : authorizedGroups)
                {
                    getSubGroupsTransitive(group, allGroups);
                }
                for (Group group : allGroups) {
                    if (group.isAnon()) {
                        visibleTo.add("public");
                    } else if (Globus.isGlobusGroup(group.getName())) {
                        visibleTo.add(Globus.getUnPrefixedGroupID(group.getName()));
                    }
                }
            }
            return visibleTo.toArray(new String[0]);
        }
        catch (SQLException e)
        {
            log.error("Unable to get groups " + e);
        }
        return null;
    }

    /**
     * @param item
     * @return
     */
    private JSONObject gmetaContentForItem(Context context, 
            Item item, Collection coll)
    {
        JSONObject content = Globus.getAllMetadataAsJSONObject(context, item, 
                coll);
        // Add the resourceType
        content.put("https://schema.labs.datacite.org/meta/kernel-4.0/metadata.xsd#resourceTypeGeneral", 
                "dataset");
        // Do a brute-force filtering of content that it isn't in the right format
        List<String> keysToRemvoe = new ArrayList<String>();
        for (Object keyObj : content.keySet())
        {
            String key = keyObj.toString();
            // Simple check for containing a URI (based on #) or a context
            // reference (containing :) or a special string 
            if (! (key.contains("#") || key.contains(":") ||
                    key.startsWith("@"))) {
                keysToRemvoe.add(key);
            }
        }
        for (String key : keysToRemvoe)
        {
            content.remove(key);
        }
        return content;
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#indexContent(org.dspace.core.Context, org.dspace.content.DSpaceObject)
     */
    @Override
    public void indexContent(Context context, DSpaceObject dso)
            throws SQLException
    {
        indexContent(context, dso, false);
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#indexContent(org.dspace.core.Context, org.dspace.content.DSpaceObject, boolean)
     */
    @Override
    public void indexContent(Context context, DSpaceObject dso, boolean force)
            throws SQLException
    {
        SQLException superException = null;
        try {
            super.indexContent(context, dso, force);
        } catch (SQLException sqle) {
            superException = sqle;
        }
        // We are indexing, so we want to do it as the publish user, so don't
        // pass the context even though we have it        
        GlobusDataSearchClient searchClient = getSearchClient(null);
        int dsoType = dso.getType();
        Collection coll = null;
        if (dsoType == Constants.ITEM) {
            Item item = (Item) dso;
            coll = item.getOwningCollection();
            JSONObject content = gmetaContentForItem(context, item, coll);
            String[] visibleTo = visibleToListForDSpaceObject(context, dso);
            String subject = subjectForItem(item);
            try {
                searchClient.index(content, subject, visibleTo);
            } catch (GlobusClientException gce) {
                log.error("Index operation failed on " + gce);
            }
        } else {
            log.info("Not indexing non-Item object: " + dso);
        }
        if (superException != null) {
            log.info("Re-raising Solr indexing exception: " + superException);
            throw superException;
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#indexContent(org.dspace.core.Context, org.dspace.content.DSpaceObject, boolean, boolean)
     */
    @Override
    public void indexContent(Context context, DSpaceObject dso, boolean force,
            boolean commit) throws SQLException, SearchServiceException
    {
        super.indexContent(context, dso, force, commit);
        log.info("indexContent on " + dso + " for context " + context);
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#unIndexContent(org.dspace.core.Context, org.dspace.content.DSpaceObject)
     */
    @Override
    public void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException
    {
        super.unIndexContent(context,  dso, true);
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#unIndexContent(org.dspace.core.Context, org.dspace.content.DSpaceObject, boolean)
     */
    @Override
    public void unIndexContent(Context context, DSpaceObject dso,
            boolean commit) throws SQLException, IOException
    {
        log.info("unIndexContent on " + dso);
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#unIndexContent(org.dspace.core.Context, java.lang.String)
     */
    @Override
    public void unIndexContent(Context context, String handle)
            throws SQLException, IOException
    {
        super.unIndexContent(context, handle, true);
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#unIndexContent(org.dspace.core.Context, java.lang.String, boolean)
     */
    @Override
    public void unIndexContent(Context context, String handle, boolean commit)
            throws SQLException, IOException
    {
        log.info("unIndex content on handle: " + handle);
    }

    /* (non-Javadoc)
     * @see org.dspace.discovery.IndexingService#reIndexContent(org.dspace.core.Context, org.dspace.content.DSpaceObject)
     */
    @Override
    public void reIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException
    {
        log.info("reIndex for dso " + dso);
    }
}
