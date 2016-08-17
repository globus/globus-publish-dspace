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
package org.dspace.handle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.globus.Globus;
import org.dspace.globus.GlobusHandleClient;
import org.dspace.globus.configuration.BaseConfigurable;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.GlobusConfigurationManager;
import org.dspace.identifier.HandleIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 *
 */
public class HandleServerIdentifierProvider extends HandleIdentifierProvider
        implements Configurable
{
    private static final Logger logger = Logger
            .getLogger(HandleServerIdentifierProvider.class);

    private static final String CFG_AUTH_INDEX = "handle.auth-index";

    private static final String CFG_KEY_PASSCODE = "handle.key-passcode";

    private static final String CFG_PRIVATE_KEY = "handle.private-key";

    private static final String CFG_PREFIX = "handle.prefix";

    private static final String CFG_NAMESPACE = "handle.namespace";

    public static final String CFG_RESOLVER = "handle.resolver";

    /**
     * Make this globus so we can reference it in call to
     * {@link GlobusConfigurationManager#getProperty(Context, DSpaceObject, org.dspace.globus.configuration.Configurable.ConfigurableProperty)
     * } to handle the default value for us
     */
    private static final ConfigurableProperty RESOLVER_PROPERTY = new ConfigurableProperty(
            CFG_RESOLVER, "Resolver base URL ", "http://hdl.handle.net/",
            DataType.STRING, "Base URL for resolving identifier", true);

    private static final ConfigurableProperty[] props = new ConfigurableProperty[] {
        IdentifierProvider.IdentifierConfigProp,
            new ConfigurableProperty(CFG_PREFIX, "Prefix", "123456789",
                    DataType.STRING, "Handle Prefix", true),
            new ConfigurableProperty(CFG_NAMESPACE, "Namespace", null, DataType.STRING, "Handle namespace", true),
            new ConfigurableProperty(CFG_PRIVATE_KEY,
                    "Administration Private Key", null, DataType.BLOB, "Base64 encoded string of the admin. private key", true),
            new ConfigurableProperty(CFG_KEY_PASSCODE, "Private Key Passcode",
                    null, DataType.PASSWORD, "Passcode for the private key", true),
            new ConfigurableProperty(CFG_AUTH_INDEX, "Authorization Index",
                    300, DataType.INTEGER, "Auth. index for provided admin. key", true), RESOLVER_PROPERTY };

    private Map<String, GlobusHandleClient> clientMap;

    private BaseConfigurable config;

    protected String[] supportedPrefixes = new String[]{"info:hdl", "hdl", "http://hdl", "https://hdl"};

    /**
     *
     */
    public HandleServerIdentifierProvider()
    {
        config = new BaseConfigurable("Handle", props);
    }

    // modified to not match all http:// identifiers
    public boolean supports(String identifier)
    {
        for(String prefix : supportedPrefixes){
            if(identifier.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
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

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.HandleIdentifierProvider#reserve(org.dspace.core
     * .Context, org.dspace.content.DSpaceObject, java.lang.String)
     */
    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
    {
        // TODO Auto-generated method stub
        super.reserve(context, dso, identifier);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.HandleIdentifierProvider#resolve(org.dspace.core
     * .Context, java.lang.String, java.lang.String[])
     */
    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes)
    {
        // TODO Auto-generated method stub
        return super.resolve(context, identifier, attributes);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.HandleIdentifierProvider#lookup(org.dspace.core
     * .Context, org.dspace.content.DSpaceObject)
     */
    @Override
    public String lookup(Context context, DSpaceObject dso)
            throws IdentifierNotFoundException,
            IdentifierNotResolvableException
    {
        // TODO Auto-generated method stub
        return super.lookup(context, dso);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.HandleIdentifierProvider#delete(org.dspace.core
     * .Context, org.dspace.content.DSpaceObject, java.lang.String)
     */
    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        // TODO Auto-generated method stub
        super.delete(context, dso, identifier);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.HandleIdentifierProvider#delete(org.dspace.core
     * .Context, org.dspace.content.DSpaceObject)
     */
    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        // TODO Auto-generated method stub
        super.delete(context, dso);
    }

    /**
     * This method is copied from the parent class from dspace so we can
     * override the call to populateHandleMetadata with the local context to get
     * value from this instance's resolver.
     */
    @Override
    public String register(Context context, DSpaceObject dso)
    {
        try
        {
            String id = mint(context, dso);

            // move canonical to point the latest version
            if (dso instanceof Item)
            {
                Item item = (Item) dso;
                populateHandleMetadata(context, item, id);
            }

            return id;
        }
        catch (Exception e)
        {
            logger.error(LogManager.getHeader(context,
                    "Error while attempting to create handle", "Item id: "
                            + dso.getID()), e);
            throw new RuntimeException(
                    "Error while attempting to create identifier for Item id: "
                            + dso.getID(), e);
        }
    }

    @Override
    public String mint(Context context, DSpaceObject dso) {
        String identifier = super.mint(context, dso);
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            identifier = getCanonicalForm(context, item, identifier);
        }
        return identifier;
    }

    /**
     * This method is copied from the parent class from dspace so we can
     * override the call to populateHandleMetadata with the local context to get
     * value from this instance's resolver.
     */
    @Override
    public void register(Context context, DSpaceObject dso, String identifier)
    {
        try
        {
            createNewIdentifier(context, dso, identifier);
            if (dso instanceof Item)
            {
                Item item = (Item) dso;
                populateHandleMetadata(context, item, identifier);
            }
        }
        catch (Exception e)
        {
            logger.error(LogManager.getHeader(context,
                    "Error while attempting to create handle", "Item id: "
                            + dso.getID()), e);
            throw new RuntimeException(
                    "Error while attempting to create identifier for Item id: "
                            + dso.getID(), e);
        }
    }

    private synchronized GlobusHandleClient getHandleClientForObject(
            Context context, DSpaceObject dso)
    {
        String prefix = getPrefixForObject(context, dso);
        if (clientMap == null)
        {
            clientMap = new HashMap<String, GlobusHandleClient>();
        }
        GlobusHandleClient ghc = clientMap.get(prefix);
        if (ghc == null)
        {
            byte[] privKey = (byte[]) getConfigProperty(
                    context, dso, config.getProperty(CFG_PRIVATE_KEY));
            String passcode = getKeyPasscodeForObject(context, dso);
            /*
             * String passcode = GlobusConfigurationManager.getProperty(context,
             * CFG_KEY_PASSCODE, dso);
             */
            String authIndexStr = getConfigProperty(
                    context, CFG_AUTH_INDEX, dso);
            int authIndex = Integer.valueOf(authIndexStr);

            try
            {
                ghc = new GlobusHandleClient(privKey, null, authIndex,
                        passcode, prefix);
                clientMap.put(prefix, ghc);
            }
            catch (Exception e)
            {
                logger.error(
                        "Failed to get client handle for prefix " + prefix, e);
            }
        }
        return ghc;
    }

    /**
     * Different objects stored in different collections may use different
     * prefixes. For now, we use the global value set in the config file.
     *
     * @param context
     * @param dso
     * @return
     */
    private String getPrefixForObject(Context context, DSpaceObject dso)
    {
        String prefix = getConfigProperty(context,
                CFG_PREFIX, dso);
        return prefix;
    }

    private String getNamespaceForObject(Context context, DSpaceObject dso)
    {
        String namespace = getConfigProperty(context, CFG_NAMESPACE, dso);
        return namespace;
    }

    /**
     * @param context
     * @param dso
     * @return
     */
    private String getKeyPasscodeForObject(Context context, DSpaceObject dso)
    {
        String passcode = getConfigProperty(context,
                CFG_KEY_PASSCODE, dso);
        return passcode;
    }

    /**
     * @param context
     * @param dso
     * @return
     */
    private String getKeyfileForObject(Context context, DSpaceObject dso)
    {
        String keyFile = Globus
                .getGlobusConfigProperty("handle.private-keyfile");
        return keyFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.HandleIdentifierProvider#createNewIdentifier(org
     * .dspace.core.Context, org.dspace.content.DSpaceObject, java.lang.String)
     */
    @Override
    protected String createNewIdentifier(Context context, DSpaceObject dso,
            String handleId) throws SQLException
    {
        if (handleId == null)
        {
            handleId = createHandleId(context, dso);
        }

        TableRow handleRow = findHandleInternal(context, handleId);

        // Create the new row in the handle table
        if (handleRow == null)
        {
            handleRow = DatabaseManager.create(context, "Handle");
        }
        modifyHandleRecord(context, dso, handleRow, handleId);

        GlobusHandleClient ghc = getHandleClientForObject(context, dso);

        if (ghc != null)
        {
            String urlString = HandleManager.resolveToURL(context, handleId);
            if (urlString != null)
            {
                URL url;
                try
                {
                    url = new URL(urlString);
                    ghc.createHandle(handleId, url);
                }
                catch (Exception e)
                {
                    logger.error("Failed to create handle for object " + dso, e);
                }
            }
        }

        return handleId;
    }

    protected static String getCanonicalForm(Context context, Item item,
            String handle)
    {

        // Let the admin define a new prefix, if not then we'll use the
        // CNRI default. This allows the admin to use "hdl:" if they want to or
        // use a locally branded prefix handle.myuni.edu.
        String handlePrefix = (String) getConfigProperty(context, item, RESOLVER_PROPERTY);
        /*
        String handlePrefix = ConfigurationManager
                .getProperty("handle.canonical.prefix");
                */
        if (handlePrefix == null || handlePrefix.length() == 0)
        {
            handlePrefix = "http://hdl.handle.net/";
        }

        return handlePrefix + handle;
    }

    private final static long XorMask = 0x88888888L;

    /**
     * @param dso
     * @return
     */
    private String createHandleId(Context context, DSpaceObject dso)
    {
        int objId = dso.getID();
        int objType = dso.getType();
        int serverId = Globus.getServerId();
        long idLong = serverId + (objType << 16) + (objId << 32);
        String suffix = longToAlphaNumeric(idLong ^ XorMask);
        String namespace = getNamespaceForObject(context, dso);
        if (namespace != null && !"".equals(namespace)) {
            suffix = namespace + "_" + suffix;
        }
        return getPrefixForObject(context, dso) + "/" + suffix;
    }

    private final static String charMap = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * @param bitRep
     * @return
     */
    private static String longToAlphaNumeric(long bitRep)
    {
        StringBuffer buf = new StringBuffer();
        int numChars = charMap.length();
        while (bitRep > 0)
        {
            // System.out.println("BitRep: " + bitRep);
            buf.append(charMap.charAt((int) (bitRep % numChars)));
            bitRep = bitRep / numChars;
        }
        return buf.toString();
    }

    protected void populateHandleMetadata(Context context, Item item,
            String handle) throws SQLException, IOException, AuthorizeException
    {
        String handleref = getCanonicalForm(context, item, handle);

        // Add handle as identifier.uri DC value.
        // First check that identifier doesn't already exist.
        boolean identifierExists = false;
        DCValue[] identifiers = item.getDC("identifier", "uri", Item.ANY);
        for (DCValue identifier : identifiers)
        {
            if (handleref.equals(identifier.value))
            {
                identifierExists = true;
            }
        }
        if (!identifierExists)
        {
            item.addDC("identifier", "uri", null, handleref);
        }
    }

}
