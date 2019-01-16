/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.globus.identifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.globus.configuration.BaseConfigurable;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.GlobusConfigurationManager;
import org.dspace.globus.configuration.ObjectConfigurable;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.identifier.IdentifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 */
public class GlobusIdentifierService implements IdentifierService
{
    private static final Logger logger = Logger
            .getLogger(GlobusIdentifierService.class);

    public static final String CFG_IDENTIFIER_NAME = "globus.identifier.name";
    public static final String CFG_PROVIDER_NAME = "globus.identifier.provider.name";


    private List<IdentifierProvider> providers;

    private IdentifierProvider systemDefaultProvider;

    private List<Configurable> providerConfigs;

    public static final String DEFAULT_PROVIDER_NAME = "Default";

    @Autowired
    @Required
    public void setProviders(List<IdentifierProvider> providers)
    {
        logger.info("Identifier provider classes set to: " + providers);
        this.providers = providers;
        providerConfigs = new ArrayList<Configurable>();
        for (IdentifierProvider provider : providers)
        {
            if (provider.equals(systemDefaultProvider))
            {
                // We don't want the default provider in the list since we treat
                // it special everywhere
                continue;
            }
            provider.setParentService(this);
            if (provider instanceof Configurable)
            {
                providerConfigs.add((Configurable) provider);
            }
            else if (ObjectConfigurable.isAnnotated(provider))
            {
                providerConfigs.add(new ObjectConfigurable(provider));
            }
        }
    }

    @Autowired
    @Qualifier("org.dspace.globus.identifier.SystemDefault")
    public void setDefaultProvider(IdentifierProvider provider)
    {
        logger.info("Default provider set to: " + provider);
        systemDefaultProvider = provider;
        provider.setParentService(this);
        systemDefaultProvider.setSystemDefaultProvider(true);
        if (providers != null && providers.contains(provider))
        {
            providers.remove(provider);
        }
    }

    /**
     * Given the nane of a configuration, get back the name of the provider that
     * actually performs the configuration.
     *
     * @param context
     *            The context for doing DB operations
     * @param dso
     *            The DSpaceObject to search relative to
     * @param configName
     *            The name of a configuration
     * @return The name of the provider that implements this configuration.
     */
    public static String getProviderNameForConfigName(Context context, DSpaceObject dso, String configName) {
        String providerName = GlobusConfigurationManager.getProperty(context, CFG_PROVIDER_NAME, configName, dso);
        return providerName;
    }


    public List<Configurable> getProviderConfigurations(Context context, DSpaceObject dso)
    {
        List<Configurable> configs = new ArrayList<Configurable>();
        Set<String> existingConfigs = GlobusConfigurationManager
                .getConfigGroups(context, dso,
                        IdentifierProvider.IDENTIFIER_CONFIGURATION_CFG_NAME);
        for (String configName : existingConfigs)
        {
            BaseConfigurable configurable = new BaseConfigurable(configName);
            configs.add(configurable);
        }
        configs.addAll(providerConfigs);
        return configs;
    }

    /**
     * Test to see if a provider configuration of a given name already exists
     * for this DSpaceObject.
     *
     * @param context
     *            The context for communicating with the DB
     * @param dso
     *            The DSpaceObject who's hierarchy to test
     * @param configName
     *            The name of a configuration to test for existence
     * @return {@code true} if a provider configuration with this name already
     *         exists, false otherwise.
     */
    public static boolean doesProviderConfigExist(Context context, DSpaceObject dso, String configName)
    {
        Set<String> existingConfigs = GlobusConfigurationManager
                .getConfigGroups(context, dso,
                        IdentifierProvider.IDENTIFIER_CONFIGURATION_CFG_NAME);
        return existingConfigs.contains(configName);
    }

    /**
     * @param dso
     * @return
     */
    public String getConfiguredProviderName(Context context, DSpaceObject dso)
    {
        String configGroup = GlobusConfigurationManager.getProperty(context,
                CFG_IDENTIFIER_NAME, null, dso);
        String providerName = GlobusConfigurationManager.getProperty(context,
                CFG_PROVIDER_NAME, configGroup, dso);

        return providerName;
    }

    public void setConfiguredProvider(Context context, DSpaceObject dso,
            String configName)
    {
        GlobusConfigurationManager.setProperty(context, CFG_IDENTIFIER_NAME,
                null, dso, configName);
    }

    /**
     * @param providerName
     * @return
     */
    public Configurable getConfigurableByName(String providerName)
    {
        if (DEFAULT_PROVIDER_NAME.equals(providerName)
                && systemDefaultProvider != null
                && systemDefaultProvider instanceof Configurable)
        {
            return (Configurable) systemDefaultProvider;
        }
        for (Configurable config : providerConfigs)
        {
            if (config.getName().equals(providerName))
            {
                return config;
            }
        }
        return null;
    }

    /**
     * @param context
     * @param dso
     * @return
     */
    public static String getIdentifierConfigNameForDso(Context context,
            DSpaceObject dso)
    {
        return GlobusConfigurationManager.getProperty(context,
                CFG_IDENTIFIER_NAME, null, dso);
    }

    public static void setIdentifierConfigGroupForDso(Context context,
            DSpaceObject dso, String configGroup)
    {
        GlobusConfigurationManager.setProperty(context, CFG_IDENTIFIER_NAME,
                null, dso, configGroup);
    }

    private IdentifierProvider getProviderForDso(Context context,
            DSpaceObject dso) throws IdentifierException
    {
        String providerName = getConfiguredProviderName(context, dso);
        Configurable config = getConfigurableByName(providerName);
        if (config instanceof IdentifierProvider)
        {
            return (IdentifierProvider) config;
        }
        else if (config instanceof ObjectConfigurable)
        {
            ObjectConfigurable objConfig = (ObjectConfigurable) config;
            return (IdentifierProvider) objConfig.getConfigObject();
        }
        else if (systemDefaultProvider != null)
        {
            return systemDefaultProvider;
        }
        throw new IdentifierException("No identifier provider configured for "
                + dso);
    }

    /**
     * Determine which of the configured providers supports a provided
     * identifier
     *
     * @param identifier
     *            The identifier to locate the IdentifierProvider for
     * @return The IdentifierProvider if one can be found, else null
     */
    private IdentifierProvider getProviderForIdentifier(String identifier)
    {
    	logger.info("Getting provider for identifier");
        for (IdentifierProvider provider : providers)
        {
            if (provider.supports(identifier))
            {
                return provider;
            }
        }
        // If we fall through to here, check if the system default one supports
        // it
        if (systemDefaultProvider != null
                && systemDefaultProvider.supports(identifier))
        {
            return systemDefaultProvider;
        }
        return null;
    }

    private List<String> identifiersForDso(DSpaceObject dso)
    {
        List<String> ids = new ArrayList<String>();
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            DCValue[] idValues = item.getMetadata("dc", "identifier", "uri",
                    null);
            if (idValues != null)
            {
                for (DCValue idValue : idValues)
                {
                    String id = idValue.value;
                    if (id != null)
                    {
                        ids.add(id);
                    }
                }
            }
        }

        return ids;
    }

    private void addIdentifierToMetadata(DSpaceObject dso, String identifier)
    {
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            List<String> ids = identifiersForDso(dso);
            if (!ids.contains(identifier))
            {
                item.addMetadata("dc", "identifier", "uri", null, identifier);
            }
        }
    }

    /**
     * Reserves identifiers for the item
     *
     * @param context
     *            dspace context
     * @param dso
     *            dspace object
     */
    public void reserve(Context context, DSpaceObject dso)
            throws AuthorizeException, SQLException, IdentifierException
    {
        IdentifierProvider provider = getProviderForDso(context, dso);
        provider.mint(context, dso);
        // Update our item
        dso.update();
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws AuthorizeException, SQLException, IdentifierException
    {
        IdentifierProvider service = getProviderForDso(context, dso);
        service.reserve(context, dso, identifier);
        // Update our item
        dso.update();
    }

    @Override
    public void register(Context context, DSpaceObject dso)
            throws AuthorizeException, SQLException, IdentifierException
    {
        IdentifierProvider provider = getProviderForDso(context, dso);
        String identifier = provider.mint(context, dso);
        if (identifier != null)
        {
            addIdentifierToMetadata(dso, identifier);
            // Update our item
//            dso.update();
        }
        else
        {
            throw new IdentifierException(
                    "Failed to create identifier for dso " + dso
                            + " using provider " + provider);
        }
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
            throws AuthorizeException, SQLException, IdentifierException
    {
        IdentifierProvider provider = getProviderForIdentifier(identifier);

        provider.register(context, object, identifier);

        // Update our item
        object.update();
    }

    @Override
    public String lookup(Context context, DSpaceObject dso,
            Class<? extends Identifier> identifier)
    {
        try
        {
            IdentifierProvider service = getProviderForDso(context, dso);
            String result = service.lookup(context, dso);
            if (result != null)
            {
                return result;
            }
        }
        catch (IdentifierException e)
        {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    public DSpaceObject resolve(Context context, String identifier)
            throws IdentifierNotFoundException,
            IdentifierNotResolvableException
    {
        IdentifierProvider service = getProviderForIdentifier(identifier);
        if (service != null)
        {
            try
            {
                DSpaceObject result = service.resolve(context, identifier);
                return result;
            }
            catch (IdentifierException e)
            {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * We interpret this to mean remove all identifiers associated with this
     * DSO.
     *
     */
    public void delete(Context context, DSpaceObject dso)
            throws AuthorizeException, SQLException, IdentifierException
    {
        List<String> ids = identifiersForDso(dso);
        for (String id : ids)
        {
            try
            {
                delete(context, dso, id);
            }
            catch (IdentifierException e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws AuthorizeException, SQLException, IdentifierException
    {
        IdentifierProvider provider = getProviderForIdentifier(identifier);
        if (provider != null)
        {
            provider.delete(context, dso, identifier);
        }
    }

    /**
     * Provide a URL that a particular DSO can be accessed by. If the DSO
     * already has a handle, we use that in generating the URL. If it does not
     * have a handle, one is generated using
     * {@link HandleManager#createHandle(Context, DSpaceObject)}.
     *
     * @param context
     * @param dso
     * @return
     */
    public static String getLandingUrlForDso(Context context, DSpaceObject dso)
    {
        String handle;
        try
        {
            handle = HandleManager.findHandle(context, dso);
            // If there's no handle
            if (handle == null)
            {
                handle = dso.getTypeText() + "/" + dso.getID();
                handle = HandleManager.createHandle(context, dso, handle);
            }
            return HandleManager.resolveToURL(context, handle);
        }
        catch (SQLException e)
        {
            logger.error("Failed to generate Url for DSO " + dso, e);
        }
        return null;
    }
}
