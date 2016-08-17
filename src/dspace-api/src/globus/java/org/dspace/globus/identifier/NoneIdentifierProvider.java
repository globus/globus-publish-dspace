/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.globus.identifier;

import java.sql.SQLException;
import java.util.Iterator;

import net.swisstech.bitly.BitlyClient;
import net.swisstech.bitly.model.Response;
import net.swisstech.bitly.model.v3.ShortenResponse;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.globus.Globus;
import org.dspace.globus.configuration.BaseConfigurable;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.GlobusConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierProvider;

/**
 *
 */
public class NoneIdentifierProvider extends IdentifierProvider implements
        Configurable
{

    /**
     *
     */
    public static final String PROVIDER_NAME = "None";

    private static final Logger logger = Logger.getLogger(NoneIdentifierProvider.class);

    private static final ConfigurableProperty[] configProps = {};

    private BaseConfigurable config;

    /**
     *
     */
    public NoneIdentifierProvider()
    {
        config = new BaseConfigurable(PROVIDER_NAME, configProps);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Configurable#getName()
     */
    @Override
    public String getName()
    {
        if (!systemDefaultProvider)
        {
            return config.getName();
        }
        else
        {
            return GlobusIdentifierService.DEFAULT_PROVIDER_NAME;
        }
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
     * @see org.dspace.identifier.IdentifierProvider#supports(java.lang.Class)
     */
    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return supports(identifier.toString());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.identifier.IdentifierProvider#supports(java.lang.String)
     */
    @Override
    public boolean supports(String identifier)
    {
        if (identifier == null) {
            return false;
        }
        String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
        return identifier.contains(dspaceUrl);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#register(org.dspace.core.Context
     * , org.dspace.content.DSpaceObject)
     */
    @Override
    public String register(Context context, DSpaceObject item)
            throws IdentifierException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#mint(org.dspace.core.Context,
     * org.dspace.content.DSpaceObject)
     */
    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        return GlobusIdentifierService.getLandingUrlForDso(context, dso);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#resolve(org.dspace.core.Context,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes) throws IdentifierNotFoundException,
            IdentifierNotResolvableException
    {
        try
        {
            return HandleManager.resolveToObject(context, identifier);
        }
        catch (IllegalStateException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#lookup(org.dspace.core.Context,
     * org.dspace.content.DSpaceObject)
     */
    @Override
    public String lookup(Context context, DSpaceObject object)
            throws IdentifierNotFoundException,
            IdentifierNotResolvableException
    {
        try
        {
            String handle = HandleManager.findHandle(context, object);
            return handle;
        }
        catch (SQLException e)
        {
            logger.warn("Handle lookup of " + object + " failed", e);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#delete(org.dspace.core.Context,
     * org.dspace.content.DSpaceObject)
     */
    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#delete(org.dspace.core.Context,
     * org.dspace.content.DSpaceObject, java.lang.String)
     */
    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#reserve(org.dspace.core.Context,
     * org.dspace.content.DSpaceObject, java.lang.String)
     */
    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.identifier.IdentifierProvider#register(org.dspace.core.Context
     * , org.dspace.content.DSpaceObject, java.lang.String)
     */
    @Override
    public void register(Context context, DSpaceObject object, String identifier)
            throws IdentifierException
    {
        // Create a landing URL for the object
    	String landingUrl = GlobusIdentifierService.getLandingUrlForDso(
                context, object);
    }

}
