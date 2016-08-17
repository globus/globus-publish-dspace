/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.globus.configuration;

import java.sql.Date;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.globus.configuration.Configurable.ConfigurableProperty;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 *
 */
public class GlobusConfigurationManager
{
    /**
     *
     */
    private static final String GLOBUS_CONFIG_TABLE_NAME = "GlobusConfig";

    /**
     *
     */
    private static final String QUERY_STRING_NOGROUP = "SELECT * from GlobusConfig where dso_id=? and dso_type=? and config_name=?";

    private static final String QUERY_STRING_ALL = QUERY_STRING_NOGROUP
            + " and config_group=?";

    private static final String QUERY_STRING_DELETE_DSO_ENTRIES = "DELETE FROM "
            + GLOBUS_CONFIG_TABLE_NAME + " WHERE dso_id=? and dso_type=?";

    private static final Logger logger = Logger
            .getLogger(GlobusConfigurationManager.class);

    private static TableRowIterator getPropertyRowIterator(Context context, String configId, String configGroup, DSpaceObject dso)
    {
        if (dso == null || context == null || configId == null)
        {
            return null;
        }

        int objId = dso.getID();
        int objType = dso.getType();
        try
        {
            TableRowIterator tri = null;
            if (configGroup != null)
            {
                tri = DatabaseManager
                        .queryTable(context, GLOBUS_CONFIG_TABLE_NAME, QUERY_STRING_ALL,
                                objId, objType, configId, configGroup);
            }
            else
            {
                tri = DatabaseManager.queryTable(context, GLOBUS_CONFIG_TABLE_NAME,
                        QUERY_STRING_NOGROUP, objId, objType, configId);
            }
            return tri;
        }
        catch (SQLException e)
        {
            logger.error("Query " + QUERY_STRING_ALL + " failed on ", e);
            return null;
        }
    }

    private static TableRow getPropertyRow(Context context, String configId,
            String configGroup, DSpaceObject dso)
    {
        TableRow result = null;
        TableRowIterator tri = getPropertyRowIterator(context, configId,
                configGroup, dso);

        try
        {
            if (tri != null && tri.hasNext())
            {
                result = tri.next();
            }
        }
        catch (SQLException e)
        {
            logger.error("Failed to get a row from " + tri, e);
        }
        if (tri != null)
        {
            tri.close();
        }
        return result;
    }

    /**
     * Get the value of a property as an Object of the proper type for a
     * configuration property.
     *
     * @param context
     *            The context used for doing DB operations
     * @param dso
     *            The DSpaceObject which should be used for searching for the
     *            value
     * @param prop
     *            The configurable property which should be returned. It's
     *            {@link ConfigurableProperty#id} will be used for the id to
     *            lookup and its {@link ConfigurableProperty#defaultValue} will
     *            be returned if no value is found.
     * @param configName
     *            The name of the configuration to lookup.
     * @return A value of the type indicated by the
     *         {@link ConfigurableProperty#dataType} of the parameter prop.
     */
    public static Object getProperty(Context context, DSpaceObject dso,
            ConfigurableProperty prop, String configName)
    {
        String propStr = getProperty(context, prop.id, configName, dso);
        Object propObj = null;
        if (propStr == null)
        {
            return prop.defaultValue;
        }
        switch (prop.dataType)
        {
        case BLOB:
            // For JDK 1.6
            propObj = DatatypeConverter.parseBase64Binary(propStr);
            // FOr JDK 1.8
            // propObj = Base64.getDecoder().decode(propStr);
            break;
        case INTEGER:
            propObj = Long.valueOf(propStr);
            break;
        case FLOAT:
            propObj = Double.valueOf(propStr);
            break;
        case BOOLEAN:
            propObj = Boolean.valueOf(propStr);
            break;
        case DATE:
            propObj = Date.valueOf(propStr);
            break;
        case AUTO:
        case PASSWORD:
        case STRING:
            propObj = propStr;
            break;
        case ENUM:
            // We don't know what to do here at this point, so we just return
            // the String
            propObj = propStr;
            break;
        }
        return propObj;
    }

    /**
     * Get a value for a property stored with a particular configId and within
     * the containment hierarchy of a particular DSpaceObject. If a group is
     * provided, only a value which was also stored with that group will be
     * returned.
     *
     * @param context
     *            The context to use when making database transactions.
     * @param configId
     *            The unique Id to return the value for.
     * @param configGroup
     *            The group used when storing this value. Using this name is
     *            optional.
     * @param dso
     *            The DSpaceObject to search the containment hierarchy for when
     *            retrieving the value.
     * @return The property value in String form.
     */
    public static String getProperty(Context context, String configId,
            String configGroup, DSpaceObject dso)
    {
        String propVal = getPropertyOnDso(context, configId, configGroup, dso);

        if (propVal == null)
        {
            DSpaceObject[] parentDsos = getContainingObjects(dso);
            if (parentDsos == null)
            {
                logger.error("Failed to get property value " + configId
                        + " on object " + dso);
                return null;
            }
            else
            {
                for (DSpaceObject parentDso : parentDsos)
                {
                    propVal = getProperty(context, configId, configGroup,
                            parentDso);
                    if (propVal != null)
                    {
                        break;
                    }
                }
            }
        }
        return propVal;
    }

    /**
     * Get a value for a property stored with a particular configId on a
     * particular DSpaceObject. Unlike
     * {@link #getProperty(Context, DSpaceObject, ConfigurableProperty, String)}
     * , the containment hierarchy is not searched. If a group is provided, only
     * a value which was also stored with that group will be returned.
     *
     * @param context
     *            The context to use when making database transactions.
     * @param configId
     *            The unique Id to return the value for.
     * @param configGroup
     *            The group used when storing this value. Using this name is
     *            optional.
     * @param dso
     *            The DSpaceObject to search for when retrieving the value.
     * @return The property value in String form.
     */
    public static String getPropertyOnDso(Context context, String configId,
            String configGroup, DSpaceObject dso)
    {
        TableRow result = getPropertyRow(context, configId, configGroup, dso);
        String propVal = null;
        if (result != null) {
            propVal = result.getStringColumn("config_value");
        }
        return propVal;
    }

    /**
     * @param dso
     * @return
     */
    private static DSpaceObject[] getContainingObjects(DSpaceObject dso)
    {
        if (dso instanceof Item)
        {
            Item item = (Item) dso;
            try
            {
                // We need to merge the owning collection with the other
                // collections, but make
                // sure owning collection comes first so we honor it's prop.
                // settings first
                Collection owningCollection = item.getOwningCollection();
                Collection[] collections = item.getCollections();
                if (owningCollection != null)
                {
                    if (collections == null || collections.length == 0)
                    {
                        collections = new Collection[] { owningCollection };
                    }
                    else
                    {
                        Collection[] newCollections = new Collection[collections.length + 1];
                        System.arraycopy(collections, 0, newCollections, 1,
                                collections.length);
                        newCollections[0] = owningCollection;
                        collections = newCollections;
                    }
                }
                return collections;
            }
            catch (SQLException e)
            {
                logger.error("Failed to find collections for item " + item, e);
            }
        }
        else if (dso instanceof Collection)
        {
            Collection coll = (Collection) dso;
            try
            {
                Community[] communities = coll.getCommunities();
                return communities;
            }
            catch (SQLException e)
            {
                logger.error("Failed to get communities for collection" + coll,
                        e);
            }
        }
        else if (dso instanceof Community)
        {
            Community comm = (Community) dso;
            try
            {
                Community[] parents = comm.getAllParents();
                return parents;
            }
            catch (SQLException e)
            {
                logger.error("Failed to get parent communities for community"
                        + comm, e);
            }
        }
        else if (dso != null)
        // It's not null, but we don't know what it is
        {
            logger.error("Got unexpect type of DSpaceObject: "
                    + dso.getTypeText());
        }
        return null;
    }

    /**
     * Get the name of all config groups in the hierarchy of a DSpaceObject
     * which contain a value for the config. identifier provided.
     *
     * @param context
     *            The context used for DB operations
     * @param dso
     *            The DSpaceObject to use as the leaf of the hierarchy to search
     * @param configId
     *            The name of the config item to look for
     * @return A Set of Strings containing relevant group names
     */
    public static Set<String> getConfigGroups(Context context,
            DSpaceObject dso, String configId)
    {
        Set<String> groupNames = new HashSet<String>();
        DSpaceObject[] containingObjects = getContainingObjects(dso);
        // This will be a lame loop starting below 0 to avoid copying the array
        // of containing objects
        for (int i = -1; i < containingObjects.length; i++)
        {
            DSpaceObject dsoToRetrieve = null;
            // Signal that we want to use the original input rather than the parent list we found
            if (i == -1)
            {
                dsoToRetrieve = dso;
            }
            else
            {
                dsoToRetrieve = containingObjects[i];
            }
            TableRowIterator tri = getPropertyRowIterator(context, configId,
                    null, dsoToRetrieve);
            if (tri != null)
            {
                try
                {
                    while (tri.hasNext())
                    {
                        TableRow row = tri.next();
                        String groupName = row.getStringColumn("config_group");
                        if (groupName != null)
                        {
                            groupNames.add(groupName);
                        }
                    }
                }
                catch (SQLException e)
                {
                    logger.warn("Failed to retrieve a group name for id: "
                            + configId, e);
                }
            }
        }
        return groupNames;
    }

    /**
     * Set a value on a property.
     *
     * @param context
     *            The context used for getting connections to the database.
     * @param configId
     *            The identifier for the config value to be set. A single
     *            identifier value may have multiple values set if they are set
     *            using different config. anmes.
     * @param configGroup
     *            The group for this particular configuration value. A
     *            {@code null} value can be provided in which case this may be
     *            thought of as the "default" value being set for this
     *            particular configId.
     * @param dso
     *            The object associated with this setting of the value.
     * @param value
     *            The value to be set. For purposes of persisting the value to
     *            the DB, the value will be converted to a String.
     */
    public static void setProperty(Context context, String configId,
            String configGroup, DSpaceObject dso, Object value)
    {
        TableRow tr = getPropertyRow(context, configId, configGroup, dso);
        boolean doInsert = false;
        if (tr == null)
        {
            try
            {
                tr = DatabaseManager.row(GLOBUS_CONFIG_TABLE_NAME);
                tr.setColumn("dso_id", dso.getID());
                tr.setColumn("dso_type", dso.getType());
                tr.setColumn("config_name", configId);
                tr.setColumn("config_group", configGroup);
            }
            catch (SQLException sqle)
            {
                logger.error("Failed to create row for config property", sqle);
                return;
            }
            doInsert = true;
        }

        String valueString = valueAsString(value);
        tr.setColumn("config_value", valueString);

        try
        {
            if (doInsert)
            {
                DatabaseManager.insert(context, tr);
            }
            else
            {
                DatabaseManager.update(context, tr);
            }
        }
        catch (SQLException e)
        {
            logger.info("Error inserting/updating table row " + tr, e);
        }
    }

    /**
     * @param value
     * @return
     */
    private static String valueAsString(Object value)
    {
        // If we're given a null, we'll actually set it as an empty string. Sort
        // of a cheap way
        // of clearing a value
        if (value == null)
        {
            return "";
        }
        if (value instanceof byte[])
        {
            byte[] bytes = (byte[]) value;
            // For JDK 1.6
            return DatatypeConverter.printBase64Binary(bytes);
            // For JDK 1.8
            // return Base64.getEncoder().encodeToString(bytes);
        }
        return value.toString();
    }

    /**
     * @param context
     * @param groupName
     */
    public static void removeConfigGroup(Context context, String groupName)
    {
        try
        {
            DatabaseManager.deleteByValue(context, GLOBUS_CONFIG_TABLE_NAME,
                    "config_group", groupName);
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     *
     * @param context
     * @param dso
     */
    public static void removeConfigForDso(Context context, DSpaceObject dso)
    {
        int dso_id = dso.getID();
        int dso_type = dso.getType();
        try
        {
            DatabaseManager.updateQuery(context, QUERY_STRING_DELETE_DSO_ENTRIES, dso_id, dso_type);
        }
        catch (SQLException e)
        {
            logger.warn("Removal of config for DSpaceObject id " + dso_id + " failed", e);
        }
    }
}
