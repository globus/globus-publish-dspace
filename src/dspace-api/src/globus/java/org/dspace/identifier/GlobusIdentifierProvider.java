/**
 * This file is a modified version of a DSpace file.
 * All modifications are subject to the following copyright and license.
 * 
 * Copyright 2016 University of Chicago. All Rights Reserved.
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

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.globus.Globus;
import org.dspace.globus.GlobusWebAppIntegration;
import org.dspace.globus.configuration.BaseConfigurable;
import org.dspace.globus.configuration.BasicConfigurablePropertyValueOptions;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.ConfigurableClass;
import org.dspace.globus.configuration.ConfigurableField;
import org.dspace.handle.HandleServerIdentifierProvider;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.auth.GlobusAuthToken;
import org.globus.identifiers.IdentifiersClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for Identifiers via the Globus Identifier Service
 *
 *
 * @author pruyne
 */
@ConfigurableClass(name = "GlobusIdentifierService")
public class GlobusIdentifierProvider extends IdentifierProvider implements
        Configurable
{
    /**
     *
     */
    private static final Logger log = LoggerFactory
            .getLogger(GlobusIdentifierProvider.class);

    // Configuration property names
    static final String CFG_NAMESPACE = "identifier.globus.namespace";
    static final String CFG_PROVIDER_TYPE = "identifier.globus.providertype";
    static final String CFG_IDENTIFIER_PREFIX = "identifier.globus.prefix";    
    static final String CFG_MINT_AS_IDENTITY = "identifier.globus.mint_as";

    private static final String DOI_RESOLVER = "https://doi.org/";
    private static final String ARK_RESOLVER = "https://n2t.net/";

    private static final String[] PROVIDER_TYPES = {
        "ezid", "datacite", "globus"
    };

    private static final BasicConfigurablePropertyValueOptions
        PROVIDER_TYPE_CONFIG_OPTIONS =
        new BasicConfigurablePropertyValueOptions(PROVIDER_TYPES);
    
    private static final String[] MINT_AS_VALUES = {
        "Submitting User", "Data Publication"
    };

    private static final BasicConfigurablePropertyValueOptions
        MINT_AS_CONFIG_OPTIONS =
        new BasicConfigurablePropertyValueOptions(MINT_AS_VALUES);
    
    private static final ConfigurableProperty[] configProps = {
        IdentifierProvider.IdentifierConfigProp,
            new ConfigurableProperty(CFG_NAMESPACE, "Globus Identifiers Namespace", null,
                                     DataType.STRING,
                                     "The namespace id in the Globus Identifiers Service",
                                     true),
            new ConfigurableProperty(CFG_PROVIDER_TYPE,
                                     "Namespace Provider Type",
                                     null,
                                     DataType.STRING,
                                     PROVIDER_TYPE_CONFIG_OPTIONS),
            new ConfigurableProperty(CFG_IDENTIFIER_PREFIX,
                                     "Prefix for Identifiers", null,
                                     DataType.STRING,
                                     "An (optional) prefix to place on all identifiers",
                                     false),
            new ConfigurableProperty(CFG_MINT_AS_IDENTITY,
                                     "Identity to Create Identifer As",
                                     null,
                                     DataType.STRING,
                                     MINT_AS_CONFIG_OPTIONS
                                     ), };

    @ConfigurableField(displayName = "Globus Identifiers Namespace",
                       propId = CFG_NAMESPACE)
    public String namespaceId;

    @ConfigurableField(displayName = "Provider Type", propId = CFG_PROVIDER_TYPE)
    public String providerType;

    @ConfigurableField(displayName = "Identifier Prefix", propId = CFG_IDENTIFIER_PREFIX)
    public String identifierPrefix;

    @ConfigurableField(displayName = "Identity", propId = CFG_MINT_AS_IDENTITY)
    public String mintAsIdentity;

    // DSpace metadata field name elements
    // XXX move these to MetadataSchema or some such
    public static final String MD_SCHEMA = "dc";

    public static final String DOI_ELEMENT = "identifier";

    public static final String DOI_QUALIFIER = "uri";

    private static final String DOI_SCHEME = "doi:";

    private static final String EZID_TARGET = null;

    private static final String CFG_USER = null;

    private static final String CFG_SHOULDER = null;

    private static final String CFG_PASSWORD = null;

    private static final String CFG_PUBLISHER = null;


    private BaseConfigurable config;

    /**
     *
     */
    public GlobusIdentifierProvider()
    {
        config = new BaseConfigurable("GlobusIdentifierService", configProps);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Configurable#getName()
     */
    @Override
    public String getName()
    {
        return config.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Configurable#getPropIds()
     */
    @Override
    public Iterator<String> getPropIds()
    {
        return config.getPropIds();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Configurable#getProperty(java.lang.String
     * )
     */
    @Override
    public ConfigurableProperty getProperty(String id)
    {
        return config.getProperty(id);
    }

    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return DOI.class.isAssignableFrom(identifier);
    }

    @Override
    public boolean supports(String identifier)
    {
        if (null == identifier)
        {
            return false;
        }
        else
        {
            return identifier.startsWith(DOI_SCHEME)
                    || identifier.startsWith(DOI_RESOLVER);
        } // XXX more thorough test?
    }

    private static final String BASE62_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final BigInteger BASE62_ALPHABET_SIZE = BigInteger.valueOf(BASE62_ALPHABET.length());

    public static String base62encode(byte[] bytes)
    {
        BigInteger val = new BigInteger(1, bytes);        
        StringBuffer buf = new StringBuffer();

        while (val.compareTo(BigInteger.ZERO) != 0) {
            BigInteger[] newvals = val.divideAndRemainder(BASE62_ALPHABET_SIZE);
            val = newvals[0];
            buf.append(BASE62_ALPHABET.charAt(newvals[1].intValue()));
        }
        return buf.toString();
    }

    public static String shortishId()
    {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        return base62encode(bytes);
    }
    
    @Override
    public String register(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.info("GlobusIdentifierProvider register no id {}", dso);

        if (!(dso instanceof Item))
        {
            throw new IdentifierException("Unsupported object type "
                    + dso.getTypeText());
        }

        Item item = (Item) dso;
        DCValue[] identifiers = item.getMetadata(MD_SCHEMA, DOI_ELEMENT,
                DOI_QUALIFIER, null);
        for (DCValue identifier : identifiers)
        {
            if ((null != identifier.value) && (supports(identifier.value)))
            {
                return identifier.value;
            }
        }

        String id = mint(context, item);
        item.clearDC(DOI_ELEMENT, DOI_QUALIFIER, null);
        String uri = uriForId(context, dso, id);
        item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, uri);
        try
        {
            item.update();
            context.commit();
        }
        catch (SQLException ex)
        {
            throw new IdentifierException("New identifier not stored", ex);
        }
        catch (AuthorizeException ex)
        {
            throw new IdentifierException("New identifier not stored", ex);
        }
        log.info("Registered {}", id);
        try
        {
            modifyHandleRecord(context, dso, id);
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return id;
    }

    /**
     * Given the id portion create the full Uri to be stored in the metadata for
     * the item
     *
     * @param id
     * @return
     */
    private String uriForId(Context ctx, DSpaceObject dso, String id)
    {
        String resolver = (String) getConfigProperty(ctx,
                HandleServerIdentifierProvider.CFG_RESOLVER, dso);
        if (resolver == null) {
            resolver = DOI_RESOLVER;
        }
        if (id.startsWith(resolver))
        {
            return id;
        }
        else
        {
            return resolver + id;
        }
    }

    protected String modifyHandleRecord(Context context, DSpaceObject dso,
            String handleId) throws SQLException
    {
        TableRow handle = getHandleInternal(context, dso.getType(), dso.getID());
        if (handle == null)
        {
            handle = DatabaseManager.create(context, "Handle");
        }

        handle.setColumn("handle", handleId);
        handle.setColumn("resource_type_id", dso.getType());
        handle.setColumn("resource_id", dso.getID());
        DatabaseManager.update(context, handle);

        if (log.isDebugEnabled())
        {
            log.debug("Created new handle for "
                    + Constants.typeText[dso.getType()] + " " + handleId);
        }
        return handleId;
    }

    /**
     * Return the handle for an Object, or null if the Object has no handle.
     *
     * @param context
     *            DSpace context
     * @param type
     *            The type of object
     * @param id
     *            The id of object
     * @return The handle for object, or null if the object has no handle.
     * @exception java.sql.SQLException
     *                If a database error occurs
     */
    protected static TableRow getHandleInternal(Context context, int type,
            int id) throws SQLException
    {
        String sql = "SELECT * FROM Handle WHERE resource_type_id = ? "
                + "AND resource_id = ?";

        return DatabaseManager.querySingleTable(context, "Handle", sql, type,
                id);
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
    {
        log.debug("GlobusIdentifierProvider register {} as {}", object, identifier);
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("GlobusIdentifierProvider reserve {}", identifier);
    }

    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        Item item = null;
        if (dso instanceof Item) {
            item = (Item) dso;
        } else {
            throw new IdentifierException("Can only mint identifiers for Items, not for " + dso);
        }
        log.debug("mint for {}", dso);
        String providerType = getConfigProperty(context, CFG_PROVIDER_TYPE, dso);
        String identifierNamespace = getConfigProperty(context, CFG_NAMESPACE, dso);
        String prefix = getConfigProperty(context, CFG_IDENTIFIER_PREFIX, dso);
        String mintIdentity = getConfigProperty(context, CFG_MINT_AS_IDENTITY, dso);
        boolean asUser = mintIdentity.equalsIgnoreCase(MINT_AS_VALUES[0]);
       
        log.info("mint params: {}, {}, {}", new Object[] {providerType, identifierNamespace, mintIdentity});
        GlobusClient gc = null;
        if (asUser) {
            Globus g = Globus.getGlobusClientFromContext(context);
            gc = g.getClient();
        } else {
            gc = Globus.getPrivlegedClient();
        }
        GlobusAuthToken identifiersToken = gc.getAuthTokenForRequestType(RequestType.identifiers);
        if (identifiersToken == null) {
            throw new IdentifierException("Could not get token for invoking identifiers Service ");
        }
        IdentifiersClient idsClient = new IdentifiersClient(identifiersToken);
        org.globus.identifiers.Identifier identifier = new org.globus.identifiers.Identifier();
        try {
            String[] visibleTo = Globus.visibleToListForDSpaceObject(context,
                                                                     dso, null);            
            identifier.visibleTo = visibleTo;
            String epName = item.getGlobusEndpoint();
            String epPath = item.getGlobusSharePath();            
            String location =
                GlobusWebAppIntegration.getWebAppFileManagerUrl(epName,
                                                                epPath,
                                                                "origin");
            identifier.location = new String[] {location};
            switch(providerType) {
                case "ezid":
                    identifier.metadata = getEZIDMetadata(item);
                    break;
                case "globus":
                    identifier.metadata = Globus.getAllMetadataAsMap(context, item, null);
                    break;
                case "datacite":
                    identifier.metadata = getDataciteMetadata(item);
                    break;
            }
            String identifierVal = null;
            if (prefix != null && prefix.length() > 0) {
                identifierVal = prefix + "." + shortishId();
            }
            identifier = idsClient.createIdentifier(identifierNamespace,
                                                    identifier, identifierVal);
            String identifierUri = getIdentifierUri(providerType, identifier);
            modifyHandleRecord(context, dso, identifier.identifier);
            return identifierUri;
        } catch (GlobusClientException | SQLException e) {
            log.error("Failed to create identifier due to " + e);
            throw new IdentifierException(e);
        }
    }

    private String getIdentifierUri(String providerType,
                                    org.globus.identifiers.Identifier identifier)
    {
        String identifierUri = null;
        switch(providerType) {
        case "ezid":
            identifierUri = ARK_RESOLVER + identifier.identifier;
            break;
        case "globus":
            identifierUri = identifier.landingPage;
            break;
        case "datacite":
            identifierUri = DOI_RESOLVER + identifier.identifier;
            break;
        }
        return identifierUri;
    }

    
    private String setSingleMapValFromItemMetadata(Map<String, Object> map,
                                                 String mapKey,
                                                 Item item,
                                                 String metadataSchema,
                                                 String metadataElement,
                                                 String metadataQualifier,
                                                 String metadataLanguage,
                                                 boolean required)
        throws IdentifierException
    {
        DCValue[] mdVals = item.getMetadata(metadataSchema, metadataElement,
                                            metadataQualifier, metadataLanguage);
        if (mdVals == null || mdVals.length == 0) {
            if (required) {
                throw new IdentifierException("Metadata value " + metadataSchema +
                                              "." + metadataElement + "." +
                                              metadataQualifier +
                                              " not found in item " + item);
            } else {
                return null;
            }
        } else {
            map.put(mapKey, mdVals[0].value);
            return mdVals[0].value;
        }
    }

    private void setListMapValuesFromItemMetadata(Map<String, Object> map,
                                                  String mapKey, Item item,
                                                  String metadataSchema,
                                                  String metadataElement,
                                                  String metadataQualifier,
                                                  String metadataLanguage,
                                                  boolean required) throws IdentifierException
    {
        DCValue[] mdVals = item.getMetadata(metadataSchema, metadataElement,
                                            metadataQualifier, metadataLanguage);
        if (mdVals == null || mdVals.length == 0) {
            if (required) {
                throw new IdentifierException("Metadata value " + metadataSchema +
                                              "." + metadataElement + "." +
                                              metadataQualifier +
                                              " not found in item " + item);
            }
        } else {
            List<Object> valList = new ArrayList<>();
            for (DCValue mdVal : mdVals) {
                valList.add(mdVal.value);
            }
            map.put(mapKey, valList);
        }
    }

    private Map<String, Object> getChildMap(Map<String, Object> parentMap,
                                            String childPath)
    {
        String[] parts = childPath.split("\\.", 2);
        Map<String, Object> childMap = (Map<String, Object>)
            parentMap.getOrDefault(parts[0], new HashMap<String, Object>());
        if (parts.length > 1) {
            childMap = getChildMap(childMap, parts[1]);
        }
        parentMap.put(parts[0], childMap);
        return childMap;
    }

    /**
     * @param metadataQualifierPropName Where we used qualifier to encode a
     * property of a nested structure/object when encoding to the flat DSpace
     * metadata scheme this is the name of the property we should put the
     * qualifier value into the returned object. For this to work, the
     * metadataQualifier parameter should be passed as Item.ANY so that metadata
     * lookup returns values regardless of the qualifier value.
     */
    private List<Map<String, Object>> makeListOfMaps(String mapKey, Item item, 
                                                     String metadataSchema,
                                                     String metadataElement,
                                                     String metadataQualifier,
                                                     String metadataLanguage,
                                                     boolean required,
                                                     String metadataQualifierPropName
                                                     ) throws IdentifierException
    {
        List<Map<String, Object>> retList = new ArrayList<>();

        DCValue[] mdVals = item.getMetadata(metadataSchema, metadataElement,
                                            metadataQualifier, metadataLanguage);
        if (mdVals == null || mdVals.length == 0) {
            if (required) {
                throw new IdentifierException("Metadata value " + metadataSchema +
                                              "." + metadataElement + "." +
                                              metadataQualifier +
                                              " not found in item " + item);
            }
        } else {
            for (DCValue mdVal : mdVals) {
                String value = mdVal.value;
                Map<String, Object> metadataMap = new HashMap<>();
                metadataMap.put(mapKey, value);
                if (metadataQualifierPropName != null) {
                    String qualifier = mdVal.qualifier;
                    if (qualifier != null && qualifier.length() > 0) {
                        metadataMap.put(metadataQualifierPropName, qualifier);
                    }
                }
                retList.add(metadataMap);
            }
        }

        return retList;
    }
    
    private Map<String, Object> getDataciteMetadata(Item item) throws IdentifierException
    {
        Map<String, Object> metadataMap = new HashMap<>();

        List<Map<String, Object>> creatorsList = makeListOfMaps("creatorName",
                                                                item,
                                                                "dc",
                                                                "contributor",
                                                                "author",
                                                                Item.ANY,
                                                                true, null);
        metadataMap.put("creators", creatorsList);

        List<Map<String, Object>> titlesList = makeListOfMaps("title",
                                                              item,
                                                              "dc",
                                                              "title",
                                                              Item.ANY,
                                                              Item.ANY,
                                                              true, null);
        metadataMap.put("titles", titlesList);

        setSingleMapValFromItemMetadata(metadataMap, "publisher", item, "dc",
                                        "publisher", Item.ANY, Item.ANY, true);

        try {
            setSingleMapValFromItemMetadata(metadataMap, "publicationYear", item,
                                            "datacite", "publicationyear", Item.ANY,
                                            Item.ANY, true);
        } catch (IdentifierException ie) {
            // We'll provide a helper here and insert the current year
            String year = String.valueOf(Calendar.getInstance().get(
                    Calendar.YEAR));
            metadataMap.put("publicationYear", year);
        }

        List<Map<String, Object>> subjectsList = makeListOfMaps("subject",
                                                                item,
                                                                "dc",
                                                                "title",
                                                                Item.ANY,
                                                                Item.ANY, false,
                                                                null);
        if (subjectsList.size() > 0) {
            metadataMap.put("subjects", subjectsList);
        }

        List<Map<String, Object>> contributorsList = makeListOfMaps("contributorName",
                                                                    item,
                                                                    "datacite",
                                                                    "contributor",
                                                                    Item.ANY,
                                                                    Item.ANY, false,
                                                                    "contributorType");
        if (contributorsList.size() > 0) {
            metadataMap.put("contributors", contributorsList);
        }

        setSingleMapValFromItemMetadata(metadataMap, "langugage", item, "dc",
                                        "language", Item.ANY, Item.ANY, false);

        Map<String, Object> resourceTypeMap = getChildMap(metadataMap, "resourceType");
        setSingleMapValFromItemMetadata(resourceTypeMap,
                                        "resourceTypeGeneral", item,
                                        "datacite", "resourcetype",
                                        Item.ANY, Item.ANY, true);

        List<Map<String, Object>> relatedIdentifierList = makeListOfMaps("relatedIdentifier",
                                                                    item,
                                                                    "datacite",
                                                                    "relationtype",
                                                                    Item.ANY,
                                                                    Item.ANY, false,
                                                                    "relationType");
        if (relatedIdentifierList.size() > 0) {
            // The Schema requires a value for relatedIdentifierType, but our
            // messed up mapping doesn't provide that due to lack of fields
            // on a metadata value. So, we're just going to hard code DOI
            // as we know we're minting a datacite DOI so that's the best guess
            // for a related identifier's type as well. Yuck!
            for (Map<String, Object> related : relatedIdentifierList) {                
                related.put("relatedIdentifierType", "DOI");
            }
            metadataMap.put("relatedIdentifiers", relatedIdentifierList);
        }

        setListMapValuesFromItemMetadata(metadataMap, "sizes", item,
                                         "datacite", "size", Item.ANY, Item.ANY,
                                         false);

        setListMapValuesFromItemMetadata(metadataMap, "formats", item,
                                         "datacite", "format", Item.ANY, Item.ANY,
                                         false);

        setListMapValuesFromItemMetadata(metadataMap, "versions", item,
                                         "datacite", "version", Item.ANY, Item.ANY,
                                         false);

        List<Map<String, Object>> rightsList = makeListOfMaps("rights",
                                                              item,
                                                              "datacite",
                                                              "rights",
                                                              Item.ANY,
                                                              Item.ANY, false,
                                                              "rightsURI");
        if (rightsList.size() > 0) {
            metadataMap.put("rightsList", rightsList);
        }

        List<Map<String, Object>> descriptionsList = makeListOfMaps("description",
                                                                    item,
                                                                    "datacite",
                                                                    "descriptionType",
                                                                    Item.ANY,
                                                                    Item.ANY, false,
                                                                    "descriptionType");
        if (descriptionsList.size() > 0) {
            metadataMap.put("descriptions", descriptionsList);
        }

        return metadataMap;
    }

    private Map<String, Object> getEZIDMetadata(Item item) throws IdentifierException
    {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("_profile", "erc");
        setSingleMapValFromItemMetadata(metadata, "erc.what", item, "dc",
                                        "title", Item.ANY, Item.ANY, true);
        setSingleMapValFromItemMetadata(metadata, "erc.who", item, "dc",
                                        "contributor", "author", Item.ANY, true);
        String whenVal = setSingleMapValFromItemMetadata(metadata, "erc.when",
                                                         item,
                                                         "dc", "date",
                                                         "issued", Item.ANY,
                                                         false);
        if (whenVal == null) {
            setSingleMapValFromItemMetadata(metadata, "erc.when",
                                            item,
                                            "datacite", "publicationyear",
                                            Item.ANY, Item.ANY,
                                            false);            
        }
        return metadata;
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes) throws IdentifierNotFoundException,
            IdentifierNotResolvableException
    {
        log.warn("NOT IMPLEMENTED resolve {}", identifier);
        return null;
    }

    @Override
    public String lookup(Context context, DSpaceObject object)
            throws IdentifierNotFoundException,
            IdentifierNotResolvableException
    {
        log.warn("NOT IMPLEMENTED lookup {}", object);
        return null;
    }

    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.debug("NOT IMPLEMENTED delete {}", dso);

    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("NOT IMPLEMENTED delete {} from {}", identifier, dso);
    }

}
