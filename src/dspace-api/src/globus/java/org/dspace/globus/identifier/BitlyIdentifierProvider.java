/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.globus.identifier;

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
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierProvider;

/**
 *
 */
public class BitlyIdentifierProvider extends IdentifierProvider implements
        Configurable
{

    /**
     *
     */
    public static final String PROVIDER_NAME = "Bitly";

    private static final Logger logger = Logger
            .getLogger(BitlyIdentifierProvider.class);

    // Config Settings
    private static final String CFG_AUTHTOKEN = "identifier.bitly.authToken";

    private static final String CFG_URL_PREFIX = "identifier.bitly.url.prefix";

    private static final String DEFAULT_URL_PREFIX = "http://bit.ly/";

    private static final ConfigurableProperty[] configProps = {
            IdentifierProvider.IdentifierConfigProp,
            new ConfigurableProperty(CFG_AUTHTOKEN, "Bit.ly API Auth Token",
                    null, DataType.STRING,
                    "Authorzation token obtained from Bit.ly dev. interface",
                    true),
            new ConfigurableProperty(CFG_URL_PREFIX, "URL Prefix",
                    DEFAULT_URL_PREFIX, DataType.STRING,
                    "Base URL to resolve identifier", true) };

    private BaseConfigurable config;

    /**
     *
     */
    public BitlyIdentifierProvider()
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

    public String getAuthToken(Context context, DSpaceObject dso)
    {
        String identifierConfigName = GlobusIdentifierService
                .getIdentifierConfigNameForDso(context, dso);
        String authToken = null;
        // If configuration says use "default" provider or there's no provider configured at all
        // and this instance of the provider is considered to be system default, get the value from
        // the global config.
        if ((GlobusIdentifierService.DEFAULT_PROVIDER_NAME
                .equals(identifierConfigName) || identifierConfigName == null)
                && systemDefaultProvider)
        {
            authToken = ConfigurationManager.getProperty(
                    Globus.GLOBUS_AUTH_CONFIG_MODULE, CFG_AUTHTOKEN);
            if (authToken == null)
            {
                logger.error("Could not get Bit.ly auth token for default setting");
            }
        }
        else
        {
            authToken = (String) getConfigProperty(context, CFG_AUTHTOKEN, dso);
            if (authToken == null)
            {
                logger.error("Missing bit.ly provider auth token for configuration "
                        + identifierConfigName);
            }
        }
        return authToken;
    }

    public String getUrlPrefix(Context context, DSpaceObject dso)
    {
        String urlPrefix = (String) getConfigProperty(context,
                CFG_URL_PREFIX, dso);
        if (!urlPrefix.endsWith("/"))
        {
            urlPrefix = urlPrefix + "/";
        }
        return urlPrefix;
    }

    private BitlyClient getClient(Context context, DSpaceObject dso)
    {
        String authToken = getAuthToken(context, dso);
        if (authToken != null)
        {
            return new BitlyClient(authToken);
        }
        else
        {
            logger.error("No token configured for Bit.ly client for " + dso);
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.identifier.IdentifierProvider#supports(java.lang.Class)
     */
    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.identifier.IdentifierProvider#supports(java.lang.String)
     */
    @Override
    public boolean supports(String identifier)
    {
        // This isn't really right since it doesn't consider what is configured.
        // BUt, since the Dso isn't provided in the interface, there's not much
        // to do here.
        return identifier.startsWith(DEFAULT_URL_PREFIX);
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
        BitlyClient client = getClient(context, dso);

        String landingUrl = GlobusIdentifierService.getLandingUrlForDso(
                context, dso);

        Response<ShortenResponse> resp = client.shorten()
                .setLongUrl(landingUrl).call();

        if (resp != null && resp.data != null)
        {
            return resp.data.url;
        }
        else
        {
            String errorText = "";
            if (resp != null) {
                errorText = ":" + resp.status_txt;
            }
            throw new IdentifierException("Failed to create bitly link to URL:"
                    + landingUrl + errorText);
        }
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
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
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
        // Delete not supported by bit.ly...
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
