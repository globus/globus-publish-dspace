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
package org.dspace.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LicenseManager;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.globus.Globus;
import org.dspace.globus.configuration.GlobusConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Class representing a collection.
 * <P>
 * The collection's metadata (name, introductory text etc), workflow groups, and
 * default group of submitters are loaded into memory. Changes to metadata are
 * not written to the database until <code>update</code> is called. If you
 * create or remove a workflow group, the change is only reflected in the
 * database after calling <code>update</code>. The default group of submitters
 * is slightly different - creating or removing this has instant effect.
 *
 * @author Robert Tansley
 */
public class Collection extends DSpaceObject
{
    public enum PolicyType {
        ADMIN("COLLECTION_${collId}_ADMIN", "authorizeManageAdminGroup", "admin", Constants.ADMIN),
        READ("COLLECTION_${collId}_DEFAULT_READERS_SUBMIT","authorizeManageSubmittersGroup", null,
                Constants.DEFAULT_ITEM_READ),
        SUBMIT("COLLECTION_${collId}_SUBMIT","authorizeManageSubmittersGroup", "submitter", Constants.ADD),
        WORKFLOW_STEP_1("COLLECTION_${collId}_WORKFLOW_STEP_1","authorizeManageWorkflowsGroup",
                "workflow_step_1", Constants.WORKFLOW_STEP_1),
        WORKFLOW_STEP_2("COLLECTION_${collId}_WORKFLOW_STEP_2", "authorizeManageWorkflowsGroup",
                "workflow_step_2", Constants.WORKFLOW_STEP_2),
        WORKFLOW_STEP_3("COLLECTION_${collId}_WORKFLOW_STEP_3", "authorizeManageWorkflowsGroup",
                "workflow_step_3", Constants.WORKFLOW_STEP_3);

        private String groupNameTemplate;

        private String changeAuthMethod;

        public String tableColumn;

        public int policyId;

        private PolicyType(String groupNameTemplate, String changeAuthMethod,
                String tableColumn, int policyId)
        {
            this.groupNameTemplate = groupNameTemplate;
            this.changeAuthMethod = changeAuthMethod;
            this.tableColumn = tableColumn;
            this.policyId = policyId;
        }

        public String getPolicyGroupName(Collection coll)
        {
            return groupNameTemplate.replace("${collId}",
                    Integer.toString(coll.getID()));
        }

        public void authorizeGroupUpdate(Context context, DSpaceObject dso)
                throws AuthorizeException
        {
            // If there's no method for making the change, we don't need to
            // authorize
            if (changeAuthMethod == null)
            {
                return;
            }
            Method authMethod = null;
            try
            {
                authMethod = AuthorizeUtil.class.getMethod(changeAuthMethod,
                        Context.class, dso.getClass());
                if (authMethod != null)
                {
                    authMethod.invoke(null, context, dso);
                }
            }
            catch (Exception e)
            {
                if (e instanceof AuthorizeException)
                {
                    throw (AuthorizeException) e;
                }
                else
                {
                    throw new AuthorizeException(e.getMessage());
                }
            }
        }

        public void authorizeAction(Context context, DSpaceObject target) throws AuthorizeException, SQLException
        {
            AuthorizeManager.authorizeAction(context, target, policyId);
        }

        /**
         * Convert a step number in to the corresponding policy type object.
         * @param step
         * @return
         */
        public static PolicyType forWorkflowStep(int step)
        {
            if (step >= 1 && step <= 3)
            {
                return PolicyType.valueOf("WORKFLOW_STEP_" + step);
            }
            else
            {
                return null;
            }
        }
    }

    /** log4j category */
    private static Logger log = Logger.getLogger(Collection.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow collectionRow;

    /** The logo bitstream */
    private Bitstream logo;

    /** The item template */
    private Item template;

    /** Our Handle */
    private String handle;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /**
     * Groups corresponding to workflow steps - NOTE these start from one, so
     * workflowGroups[0] corresponds to workflow_step_1.
     */
    private Group[] workflowGroup;

    /** The default group of submitters */
    private Group submitters;

    /** The default group of administrators */
    private Group admins;

    // Keys for accessing Collection metadata
    public static final String COPYRIGHT_TEXT = "copyright_text";

    public static final String INTRODUCTORY_TEXT = "introductory_text";

    public static final String SHORT_DESCRIPTION = "short_description";

    public static final String SIDEBAR_TEXT = "side_bar_text";

    public static final String PROVENANCE_TEXT = "provenance_description";

    // <%--Endpoint, root directory, dataset prefix, endpoint prefix, create
    // manifest, create metadata artifact--%>

    /**
     * Construct a collection with the given table row
     *
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     * @throws SQLException
     */
    Collection(Context context, TableRow row) throws SQLException
    {
        ourContext = context;
        collectionRow = row;

        // Get the logo bitstream
        if (collectionRow.isColumnNull("logo_bitstream_id"))
        {
            logo = null;
        }
        else
        {
            logo = Bitstream.find(ourContext,
                    collectionRow.getIntColumn("logo_bitstream_id"));
        }

        // Get the template item
        if (collectionRow.isColumnNull("template_item_id"))
        {
            template = null;
        }
        else
        {
            template = Item.find(ourContext,
                    collectionRow.getIntColumn("template_item_id"));
        }

        // Get the relevant groups
        workflowGroup = new Group[3];

        workflowGroup[0] = groupFromColumn("workflow_step_1");
        workflowGroup[1] = groupFromColumn("workflow_step_2");
        workflowGroup[2] = groupFromColumn("workflow_step_3");

        submitters = groupFromColumn("submitter");
        admins = groupFromColumn("admin");

        // Get our Handle if any
        handle = HandleManager.findHandle(context, this);

        // Cache ourselves
        context.cache(this, row.getIntColumn("collection_id"));

        modified = false;
        modifiedMetadata = false;
        clearDetails();
    }

    /**
     * Get a collection from the database. Loads in the metadata
     *
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the collection
     *
     * @return the collection, or null if the ID is invalid.
     * @throws SQLException
     */
    public static Collection find(Context context, int id) throws SQLException
    {
        // First check the cache
        Collection fromCache = (Collection) context.fromCache(Collection.class,
                id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "collection", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_collection",
                        "not_found,collection_id=" + id));
            }

            return null;
        }

        // not null, return Collection
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_collection",
                    "collection_id=" + id));
        }

        return new Collection(context, row);
    }

    /**
     * Create a new collection, with a new ID. This method is not public, and
     * does not check authorisation.
     *
     * @param context
     *            DSpace context object
     *
     * @return the newly created collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    static Collection create(Context context) throws SQLException,
            AuthorizeException
    {
        return create(context, null);
    }

    /**
     * Create a new collection, with a new ID. This method is not public, and
     * does not check authorisation.
     *
     * @param context
     *            DSpace context object
     *
     * @param handle
     *            the pre-determined Handle to assign to the new community
     * @return the newly created collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    static Collection create(Context context, String handle)
            throws SQLException, AuthorizeException
    {
        TableRow row = DatabaseManager.create(context, "collection");
        Collection c = new Collection(context, row);

        try
        {
            c.handle = (handle == null) ? HandleManager
                    .createHandle(context, c) : HandleManager.createHandle(
                    context, c, handle);
        }
        catch (IllegalStateException ie)
        {
            // If an IllegalStateException is thrown, then an existing object is
            // already using this handle
            // Remove the collection we just created -- as it is incomplete
            try
            {
                if (c != null)
                {
                    c.delete();
                }
            }
            catch (Exception e)
            {
            }

            // pass exception on up the chain
            throw ie;
        }

        // create the default authorization policy for collections
        // of 'anonymous' READ
        Group anonymousGroup = Group.findAnonGroup(context);


        ResourcePolicy myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        // now create the default policies for submitted items


        myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.DEFAULT_BITSTREAM_READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        context.addEvent(new Event(Event.CREATE, Constants.COLLECTION, c
                .getID(), c.handle));

        log.info(LogManager.getHeader(context, "create_collection",
                "collection_id=" + row.getIntColumn("collection_id"))
                + ",handle=" + c.handle);

        return c;
    }

    /**
     * Get all collections in the system. These are alphabetically sorted by
     * collection name.
     *
     * @param context
     *            DSpace context object
     *
     * @return the collections in the system
     * @throws SQLException
     */
    public static Collection[] findAll(Context context) throws SQLException
    {
        TableRowIterator tri = DatabaseManager.queryTable(context,
                "collection", "SELECT * FROM collection ORDER BY name");

        List<Collection> collections = new ArrayList<Collection>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Collection fromCache = (Collection) context.fromCache(
                        Collection.class, row.getIntColumn("collection_id"));

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(new Collection(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }

    /**
     * Get all collections in the system. Adds support for limit and offset.
     *
     * @param context
     * @param limit
     * @param offset
     * @return
     * @throws SQLException
     */
    public static Collection[] findAll(Context context, Integer limit,
            Integer offset) throws SQLException
    {
        StringBuffer query = new StringBuffer("SELECT * FROM collection ORDER BY name");
        List<Serializable> params = new ArrayList<Serializable>();

        DatabaseManager.applyOffsetAndLimit(query, params, offset, limit);

        TableRowIterator tri = DatabaseManager.query(
          context, query.toString(), params.toArray()
        );

        List<Collection> collections = new ArrayList<Collection>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Collection fromCache = (Collection) context.fromCache(
                        Collection.class, row.getIntColumn("collection_id"));

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(new Collection(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }

    /**
     * Get the in_archive items in this collection. The order is indeterminate.
     *
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public ItemIterator getItems() throws SQLException
    {
        String myQuery = "SELECT item.* FROM item, collection2item WHERE "
                + "item.item_id=collection2item.item_id AND "
                + "collection2item.collection_id= ? "
                + "AND item.in_archive='1'";

        TableRowIterator rows = DatabaseManager.queryTable(ourContext, "item",
                myQuery, getID());

        return new ItemIterator(ourContext, rows);
    }

    /**
     * Get the in_archive items in this collection. The order is indeterminate.
     * Provides the ability to use limit and offset, for efficient paging.
     *
     * @param limit
     *            Max number of results in set
     * @param offset
     *            Number of results to jump ahead by. 100 = 100th result is
     *            first, not 100th page.
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public ItemIterator getItems(Integer limit, Integer offset)
            throws SQLException
    {
        List<Serializable> params = new ArrayList<Serializable>();
        StringBuffer myQuery = new StringBuffer(
            "SELECT item.* " + 
            "FROM item, collection2item " + 
            "WHERE item.item_id = collection2item.item_id " +
              "AND collection2item.collection_id = ? " +
              "AND item.in_archive = '1'"
        );

        params.add(getID());
        DatabaseManager.applyOffsetAndLimit(myQuery, params, offset, limit);

        TableRowIterator rows = DatabaseManager.query(ourContext,
                myQuery.toString(), params.toArray());
        return new ItemIterator(ourContext, rows);
    }

    /**
     * Get all the items in this collection. The order is indeterminate.
     *
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public ItemIterator getAllItems() throws SQLException
    {
        String myQuery = "SELECT item.* FROM item, collection2item WHERE "
                + "item.item_id=collection2item.item_id AND "
                + "collection2item.collection_id= ? ";

        TableRowIterator rows = DatabaseManager.queryTable(ourContext, "item",
                myQuery, getID());

        return new ItemIterator(ourContext, rows);
    }

    /**
     * Get the internal ID of this collection
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return collectionRow.getIntColumn("collection_id");
    }

    /**
     * @see org.dspace.content.DSpaceObject#getHandle()
     */
    public String getHandle()
    {
        if (handle == null)
        {
            try
            {
                handle = HandleManager.findHandle(this.ourContext, this);
            }
            catch (SQLException e)
            {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
        }
        return handle;
    }

    /**
     * Get the value of a metadata field
     *
     * @param field
     *            the name of the metadata field to get
     *
     * @return the value of the metadata field
     *
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public String getMetadata(String field)
    {
        String metadata = collectionRow.getStringColumn(field);
        return (metadata == null) ? "" : metadata;
    }

    // added to get Globus config options
    public String getGlobusConfigProperty(String property)
    {
        return GlobusConfigurationManager.getProperty(ourContext, property,
                null, this);
    }

    public String getFormFile()
    {
        return Globus.getFormConfigDir()
            + File.separator
            + GlobusConfigurationManager.getProperty(ourContext, Globus.COLLECTION_INPUT_FORM_CFG_PROP, null,
                                                     this);
    }
    /**
     * Set a metadata value
     *
     * @param field
     *            the name of the metadata field to get
     * @param value
     *            value to set the field to
     *
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     * @exception MissingResourceException
     */
    public void setMetadata(String field, String value)
            throws MissingResourceException
    {
        if ((field.trim()).equals("name")
                && (value == null || value.trim().equals("")))
        {
            try
            {
                value = I18nUtil
                        .getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                value = "Untitled";
            }
        }

        /*
         * Set metadata field to null if null and trim strings to eliminate
         * excess whitespace.
         */
        if (value == null)
        {
            collectionRow.setColumnNull(field);
        }
        else
        {
            collectionRow.setColumn(field, value.trim());
        }

        modifiedMetadata = true;
        addDetails(field);
    }

    public void setBooleanMetadata(String field, boolean value)
            throws MissingResourceException
    {
        collectionRow.setColumn(field, value);
        modifiedMetadata = true;
        addDetails(field);
    }

    public String getName()
    {
        return getMetadata("name");
    }

    /**
     * Get the logo for the collection. <code>null</code> is returned if the
     * collection does not have a logo.
     *
     * @return the logo of the collection, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    /**
     * Give the collection a logo. Passing in <code>null</code> removes any
     * existing logo. You will need to set the format of the new logo bitstream
     * before it will work, for example to "JPEG". Note that <code>update</code>
     * will need to be called for the change to take effect. Setting a logo and
     * not calling <code>update</code> later may result in a previous logo lying
     * around as an "orphaned" bitstream.
     *
     * @param is
     *            the stream to use as the new logo
     *
     * @return the new logo bitstream, or <code>null</code> if there is no logo
     *         (<code>null</code> was passed in)
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    public Bitstream setLogo(InputStream is) throws AuthorizeException,
            IOException, SQLException
    {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && AuthorizeManager.authorizeActionBoolean(
                ourContext, this, Constants.DELETE)))
        {
            canEdit(true);
        }

        // First, delete any existing logo
        if (!collectionRow.isColumnNull("logo_bitstream_id"))
        {
            logo.delete();
        }

        if (is == null)
        {
            collectionRow.setColumnNull("logo_bitstream_id");
            logo = null;

            log.info(LogManager.getHeader(ourContext, "remove_logo",
                    "collection_id=" + getID()));
        }
        else
        {
            Bitstream newLogo = Bitstream.create(ourContext, is);
            collectionRow.setColumn("logo_bitstream_id", newLogo.getID());
            logo = newLogo;

            // now create policy for logo bitstream
            // to match our READ policy
            List<ResourcePolicy> policies = AuthorizeManager
                    .getPoliciesActionFilter(ourContext, this, Constants.READ);
            AuthorizeManager.addPolicies(ourContext, policies, newLogo);

            log.info(LogManager.getHeader(
                    ourContext,
                    "set_logo",
                    "collection_id=" + getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

        modified = true;
        return logo;
    }

    /**
     * Create a workflow group for the given step if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that while the new group is created in the database, the association
     * between the group and the collection is not written until
     * <code>update</code> is called.
     *
     * @param step
     *            the step (1-3) of the workflow to create or get the group for
     *
     * @return the workflow group associated with this collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Group createWorkflowGroup(int step) throws SQLException,
            AuthorizeException
    {
        Group policyGroup = createPolicyGroup(PolicyType.forWorkflowStep(step));

        // Check authorisation - Must be an Admin to create Workflow Group
        AuthorizeUtil.authorizeManageWorkflowsGroup(ourContext, this);

        // This is now redundant, and should be removed
        if (policyGroup == null && workflowGroup[step - 1] == null)
        {
            // turn off authorization so that Collection Admins can create
            // Collection Workflow Groups
            ourContext.turnOffAuthorisationSystem();
            Group g = Group.create(ourContext);
            ourContext.restoreAuthSystemState();

            g.setName("COLLECTION_" + getID() + "_WORKFLOW_STEP_" + step);
            g.update();
            setWorkflowGroup(step, g);
        } else {
            // JCP: Porting forward from our previous code
            workflowGroup[step - 1] = policyGroup;
        }

        return workflowGroup[step - 1];
    }

    /**
     * Set the workflow group corresponding to a particular workflow step.
     * <code>null</code> can be passed in if there should be no associated group
     * for that workflow step; any existing group is NOT deleted.
     *
     * @param step
     *            the workflow step (1-3)
     * @param newGroup
     *            the new workflow group, or <code>null</code>
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public void setWorkflowGroup(int step, Group newGroup)
            throws SQLException, AuthorizeException
    {
        Group oldGroup = getWorkflowGroup(step);
        String stepColumn;
        int action;
        switch(step)
        {
        case 1:
            action = Constants.WORKFLOW_STEP_1;
            stepColumn = "workflow_step_1";
            break;
        case 2:
            action = Constants.WORKFLOW_STEP_2;
            stepColumn = "workflow_step_2";
            break;
        case 3:
            action = Constants.WORKFLOW_STEP_3;
            stepColumn = "workflow_step_3";
            break;
        default:
            throw new IllegalArgumentException("Illegal step count:  " + step);
        }
        workflowGroup[step-1] = newGroup;
        if (newGroup != null)
            collectionRow.setColumn(stepColumn, newGroup.getID());
        else
            collectionRow.setColumnNull(stepColumn);
        modified = true;

        // Deal with permissions.
        try {
            ourContext.turnOffAuthorisationSystem();
            // remove the policies for the old group
            if (oldGroup != null)
            {
                List<ResourcePolicy> oldPolicies = AuthorizeManager
                        .getPoliciesActionFilter(ourContext, this, action);
                int oldGroupID = oldGroup.getID();
                for (ResourcePolicy rp : oldPolicies)
                {
                    if (rp.getGroupID() == oldGroupID)
                        rp.delete();
                }

                oldPolicies = AuthorizeManager
                        .getPoliciesActionFilter(ourContext, this, Constants.ADD);
                for (ResourcePolicy rp : oldPolicies)
                {
                    if ((rp.getGroupID() == oldGroupID)
                            && ResourcePolicy.TYPE_WORKFLOW.equals(rp.getRpType()))
                        rp.delete();
                }
           }

            // New group can be null to delete workflow step.
            // We need to grant permissions if new group is not null.
            if (newGroup != null)
            {
                AuthorizeManager.addPolicy(ourContext, this, action, newGroup,
                        ResourcePolicy.TYPE_WORKFLOW);
                /* JCP: We'd removed similar from the createWorkflowGroup method
                AuthorizeManager.addPolicy(ourContext, this, Constants.ADD, newGroup,
                        ResourcePolicy.TYPE_WORKFLOW);
                */
            }
        } finally {
            ourContext.restoreAuthSystemState();
        }
    }

    /**
     * Get the the workflow group corresponding to a particular workflow step.
     * This returns <code>null</code> if there is no group associated with this
     * collection for the given step.
     *
     * @param step
     *            the workflow step (1-3)
     *
     * @return the group of reviewers or <code>null</code>
     */
    public Group getWorkflowGroup(int step)
    {
        try
        {
            Group g = getPolicyGroup(PolicyType.forWorkflowStep(step));
            workflowGroup[step - 1] = g;
            return g;
        }
        catch (SQLException e)
        {
            log.warn("Error retrieving workflow group " + step + " from DB", e);
        }
        return null;
    }

    /**
     * C
     *
     * @return the default group of readers associated with this collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Group createReaders() throws SQLException, AuthorizeException
    {
        // Check authorisation - Must be an Admin to create default readers
        // Group
        AuthorizeUtil.authorizeManageSubmittersGroup(ourContext, this);
        Group readers = getReadPolicyGroup();

        if (readers == null)
        {
            // Remove any existing policies
            AuthorizeManager.removePoliciesActionFilter(ourContext, this,
                    Constants.DEFAULT_ITEM_READ);

            // turn off authorization so that Collection Admins can create
            // Collection Submitters
            ourContext.turnOffAuthorisationSystem();
            readers = Group.create(ourContext);
            ourContext.restoreAuthSystemState();

            readers.setName("COLLECTION_" + getID() + "_DEFAULT_READERS_SUBMIT");
            readers.update();
            AuthorizeManager.addPolicy(ourContext, this,
                    Constants.DEFAULT_ITEM_READ, readers);
        }

        return readers;
    }

    @Deprecated
    public Group getReaders() throws SQLException
    {
        Group group = getReadPolicyGroup();
        if (group != null && group.getID() == 0)
        {
            // treat anonymous group as not having a readers group yet.
            return null;
        }
        return group;
    }

    @Deprecated
    public Group getReadPolicyGroup() throws SQLException
    {
        List<ResourcePolicy> policies = AuthorizeManager
                .getPoliciesActionFilter(ourContext, this,
                        Constants.DEFAULT_ITEM_READ);
        if (policies.size() == 1)
        {
            int groupID = policies.get(0).getGroupID();
            return Group.find(ourContext, groupID);
        }
        else if (policies.size() > 1)
        {
            log.warn("There are more than 1 policies for DEFAULT_ITEM_READ");
        }
        return null;
    }

    /**
     * Remove the submitters group, if no group has already been created then
     * return without error. This will merely dereference the current submitters
     * group from the collection so that it may be deleted without violating
     * database constraints.
     */
    public void removeReaders() throws SQLException, AuthorizeException
    {
        // use the same policies for changing readers group
        // Check authorisation - Must be an Admin to delete Submitters Group
        AuthorizeUtil.authorizeManageSubmittersGroup(ourContext, this);

        Group readers = getReaders();
        // just return if there is no administrative group.
        if (readers == null)
        {
            return;
        }

        // Remove readers means we need to make public - so remove existing
        // members and add anon group
        Group[] memberGroups = readers.getMemberGroups();
        for (Group member : memberGroups)
        {
            readers.removeMember(member);
        }

        Group anonGroup = Group.findByName(ourContext, Group.ANONYMOUS_GROUP_NAME);
        readers.addMember(anonGroup);
        readers.update();
    }

    /**
     * Create a default submitters group if one does not already exist. Returns
     * either the newly created group or the previously existing one. Note that
     * other groups may also be allowed to submit to this collection by the
     * authorization system.
     *
     * @return the default group of submitters associated with this collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    public synchronized Group createSubmitters() throws SQLException,
            AuthorizeException
    {
        // Check authorisation - Must be an Admin to create Submitters Group
        AuthorizeUtil.authorizeManageSubmittersGroup(ourContext, this);
        String submittersName = "COLLECTION_" + getID() + "_SUBMIT";
        if (submitters == null)
        {
            submitters = Group.findByName(ourContext, submittersName);

            if (submitters == null)
            {
                // turn off authorization so that Collection Admins can create
                // Collection Submitters
                ourContext.turnOffAuthorisationSystem();
                submitters = Group.create(ourContext);
                ourContext.restoreAuthSystemState();

                submitters.setName(submittersName);
                submitters.update();
            }
        }

        // remove any previous members
        Group[] memberGroups = submitters.getMemberGroups();
        for (Group member : memberGroups)
        {
            submitters.removeMember(member);
        }

        // register this as the submitter group
        collectionRow.setColumn("submitter", submitters.getID());

        AuthorizeManager.addPolicy(ourContext, this, Constants.ADD, submitters);

        modified = true;
        return submitters;
    }

    /**
     * Remove the submitters group, if no group has already been created then
     * return without error. This will merely dereference the current submitters
     * group from the collection so that it may be deleted without violating
     * database constraints.
     */
    public void removeSubmitters() throws SQLException, AuthorizeException
    {
        // Check authorisation - Must be an Admin to delete Submitters Group
        AuthorizeUtil.authorizeManageSubmittersGroup(ourContext, this);

        // just return if there is no administrative group.
        if (submitters == null)
        {
            return;
        }

        // Remove means we need to make public - so remove existing members and
        // add anon group
        Group[] memberGroups = submitters.getMemberGroups();
        for (Group member : memberGroups)
        {
            submitters.removeMember(member);
        }

        Group anonGroup = Group.findByName(ourContext, Group.ANONYMOUS_GROUP_NAME);
        submitters.addMember(anonGroup);
        submitters.update();

        modified = true;
    }

    /**
     * Get the default group of submitters, if there is one. Note that the
     * authorization system may allow others to submit to the collection, so
     * this is not necessarily a definitive list of potential submitters.
     * <P>
     * The default group of submitters for collection 100 is the one called
     * <code>collection_100_submit</code>.
     *
     * @return the default group of submitters, or <code>null</code> if there is
     *         no default group.
     */
    public Group getSubmitters()
    {
        try
        {
            return getPolicyGroup(PolicyType.SUBMIT);
        }
        catch (Exception e)
        {
            log.error(
                    "Failed to get submit group for collection: " + getName(),
                    e);
        }
        return null;
        // return submitters;
    }

    public void setSubmissionPublic() throws SQLException, AuthorizeException
    {
        createSubmitters();
        if (submitters != null)
        {
            Group anonGroup = Group.findAnonGroup(ourContext);
        }
    }

    /**
     * @param policy
     * @return
     * @throws AuthorizeException
     * @throws SQLException
     */
    public synchronized Group createPolicyGroup(PolicyType policy)
            throws AuthorizeException, SQLException
    {
        Group policyGroup = getPolicyGroup(policy);
        if (policyGroup != null)
        {
            return policyGroup;
        }
        policy.authorizeGroupUpdate(ourContext, this);

        if (policy.policyId > 0)
        {
            AuthorizeManager.removePoliciesActionFilter(ourContext, this,
                    policy.policyId);
        }

        String policyGroupName = policy.getPolicyGroupName(this);
        // Look for it by name even if it wasn't set up properly in the past and we'll re-use it
        policyGroup = Group.findByName(ourContext, policyGroupName);
        if (policyGroup != null)
        {
            // But, we don't trust the old members, so we remove them
            Group[] memberGroups = policyGroup.getMemberGroups();
            for (Group member : memberGroups)
            {
                policyGroup.removeMember(member);
            }
        }
        else
        {
            ourContext.turnOffAuthorisationSystem();
            policyGroup = Group.create(ourContext);
            ourContext.restoreAuthSystemState();
            policyGroup.setName(policyGroupName);
        }

        policyGroup.update();

        // register this as the submitter group
        if (policy.tableColumn != null)
        {
            collectionRow.setColumn(policy.tableColumn, policyGroup.getID());
        }

        if (policy.policyId > 0)
        {
            AuthorizeManager.addPolicy(ourContext, this, policy.policyId,
                    policyGroup);
        }

        modified = true;
        return policyGroup;
    }

    /**
     * Get a currently configured policy group, but do not create one if it does
     * not exist.
     *
     * @param policy
     * @return The current policy group for the input policy or {@code null} if
     *         no policy group has yet been configured.
     * @throws SQLException
     */
    public Group getPolicyGroup(PolicyType policy) throws SQLException
    {
        List<ResourcePolicy> policies = AuthorizeManager
                .getPoliciesActionFilter(ourContext, this, policy.policyId);
        Group policyGroup = null;
        if (policies != null && policies.size() != 0)
        {
            int groupID = policies.get(0).getGroupID();
            if (policies.size() > 1)
            {
                log.warn("There are more than 1 policies for policy " + policy);
            }
            policyGroup = Group.find(ourContext, groupID);
        }
        log.info("Policy Group of type " + policy + " for collection " + this
                + " returning " + policyGroup);
        return policyGroup;

    }

    private Group removePolicyGroup(PolicyType policy) throws SQLException
    {
        Group prevGroup = getPolicyGroup(policy);

        AuthorizeManager.removePoliciesActionFilter(ourContext, this, policy.policyId);

        if (policy.tableColumn != null) {
            collectionRow.setColumnNull(policy.tableColumn);
        }

        return prevGroup;
    }

    public Group setPolicyTargetGroup(PolicyType policy, Group targetGroup)
            throws SQLException, AuthorizeException
    {
        Group prevGroup = getPolicyTargetGroup(policy);
        // Setting to null means remove the policy. We remove the entire policy group and return
        // the previous target group.
        if (prevGroup != null && prevGroup.equals(targetGroup))  {
            return prevGroup;
        }

        policy.authorizeGroupUpdate(ourContext, this);

        if (targetGroup == null)
        {
            removePolicyGroup(policy);
            return prevGroup;
        }

        Group policyGroup = createPolicyGroup(policy);
        if (policyGroup == null)
        {
            throw new AuthorizeException("Failed to create Group for Policy "
                    + policy);
        }

        Group[] memberGroups = policyGroup.getMemberGroups();
        if (memberGroups != null)
        {
            for (Group member : memberGroups)
            {
                policyGroup.removeMember(member);
            }
        }

        policyGroup.addMember(targetGroup);
        policyGroup.update();

        if (policy.tableColumn != null)
        {
            collectionRow.setColumn(policy.tableColumn, targetGroup.getID());
            modified = true;
        }
        return prevGroup;
    }

    public Group setPolicyTargetGroupAnonymous(PolicyType type)
            throws SQLException, AuthorizeException
    {
        return setPolicyTargetGroup(type, Group.findAnonGroup(ourContext));
    }

    public Group setPolicyTargetGroupPublic(PolicyType type)
            throws SQLException, AuthorizeException
    {
        return setPolicyTargetGroupAnonymous(type);
    }

    public Group getPolicyTargetGroup(PolicyType policy) throws SQLException
    {
        Group policyGroup;
        policyGroup = getPolicyGroup(policy);
        if (policyGroup == null)
        {
            return null;
        }
        Group[] memberGroups = policyGroup.getMemberGroups();
        Group targetGroup = null;
        if (memberGroups != null)
        {
            if (memberGroups.length == 0)
            {
                log.warn("Target group for policy " + policy
                        + " on collection " + getID() + " is empty");
            }
            else
            {
                targetGroup = memberGroups[0];
                if (memberGroups.length > 1)
                {
                    log.warn("More than one target group set on policy "
                            + policy + " for collection " + getID());
                }
            }
        }

        // Migrate forward from anything that still depends on a column set in the Collection table
        if (targetGroup == null && policy.tableColumn != null) {
            int intColumn = collectionRow.getIntColumn(policy.tableColumn);
            if (intColumn >= 0) {
                targetGroup = Group.find(ourContext, intColumn);
            }
        }
        return targetGroup;
    }

    public boolean isPolicyGroupPublic(PolicyType policy) throws SQLException
    {
        Group policyTargetGroup = getPolicyTargetGroup(policy);
        return (policyTargetGroup != null && policyTargetGroup.isAnon());
    }

    /**
     * Create a default administrators group if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that other groups may also be administrators.
     *
     * @return the default group of editors associated with this collection
     * @throws SQLException
     * @throws AuthorizeException
     */
    public Group createAdministrators() throws SQLException, AuthorizeException
    {
        // Check authorisation - Must be an Admin to create more Admins
        AuthorizeUtil.authorizeManageAdminGroup(ourContext, this);

        if (admins == null)
        {
            // turn off authorization so that Community Admins can create
            // Collection Admins
            ourContext.turnOffAuthorisationSystem();
            admins = Group.create(ourContext);
            ourContext.restoreAuthSystemState();

            admins.setName("COLLECTION_" + getID() + "_ADMIN");
            admins.update();
        }

        AuthorizeManager.addPolicy(ourContext, this, Constants.ADMIN, admins);

        // register this as the admin group
        collectionRow.setColumn("admin", admins.getID());

        modified = true;
        return admins;
    }

    /**
     * Remove the administrators group, if no group has already been created
     * then return without error. This will merely dereference the current
     * administrators group from the collection so that it may be deleted
     * without violating database constraints.
     */
    public void removeAdministrators() throws SQLException, AuthorizeException
    {
        // Check authorisation - Must be an Admin of the parent community to
        // delete Admin Group
        AuthorizeUtil.authorizeRemoveAdminGroup(ourContext, this);

        // just return if there is no administrative group.
        if (admins == null)
        {
            return;
        }

        // Remove the link to the collection table.
        collectionRow.setColumnNull("admin");
        admins = null;

        modified = true;
    }

    /**
     * Get the default group of administrators, if there is one. Note that the
     * authorization system may allow others to be administrators for the
     * collection.
     * <P>
     * The default group of administrators for collection 100 is the one called
     * <code>collection_100_admin</code>.
     *
     * @return group of administrators, or <code>null</code> if there is no
     *         default group.
     */
    public Group getAdministrators()
    {
        return admins;
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection. If the collection does not have a specific license, the
     * site-wide default is returned.
     *
     * @return the license for this collection
     */
    public String getLicense()
    {
        String license = getMetadata("license");

        if (license == null || license.trim().equals(""))
        {
            // Fallback to site-wide default
            license = LicenseManager.getDefaultSubmissionLicense();
        }

        return license;
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection.
     *
     * @return the license for this collection
     */
    public String getLicenseCollection()
    {
        return getMetadata("license");
    }

    /**
     * Find out if the collection has a custom license
     *
     * @return <code>true</code> if the collection has a custom license
     */
    public boolean hasCustomLicense()
    {
        String license = getMetadata("license");

        return !(license == null || license.trim().equals(""));
    }

    /**
     * Set the license for this collection. Passing in <code>null</code> means
     * that the site-wide default will be used.
     *
     * @param license
     *            the license, or <code>null</code>
     */
    public void setLicense(String license)
    {
        setMetadata("license", license);
    }

    /**
     * Get the template item for this collection. <code>null</code> is returned
     * if the collection does not have a template. Submission mechanisms may
     * copy this template to provide a convenient starting point for a
     * submission.
     *
     * @return the item template, or <code>null</code>
     */
    public Item getTemplateItem() throws SQLException
    {
        return template;
    }

    /**
     * Create an empty template item for this collection. If one already exists,
     * no action is taken. Caution: Make sure you call <code>update</code> on
     * the collection after doing this, or the item will have been created but
     * the collection record will not refer to it.
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void createTemplateItem() throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(ourContext, this);

        if (template == null)
        {
            template = Item.create(ourContext);
            collectionRow.setColumn("template_item_id", template.getID());

            log.info(LogManager.getHeader(ourContext, "create_template_item",
                    "collection_id=" + getID() + ",template_item_id="
                            + template.getID()));
        }
        modified = true;
    }

    /**
     * Remove the template item for this collection, if there is one. Note that
     * since this has to remove the old template item ID from the collection
     * record in the database, the collection record will be changed, including
     * any other changes made; in other words, this method does an
     * <code>update</code>.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeTemplateItem() throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(ourContext, this);

        collectionRow.setColumnNull("template_item_id");
        DatabaseManager.update(ourContext, collectionRow);

        if (template != null)
        {
            log.info(LogManager.getHeader(ourContext, "remove_template_item",
                    "collection_id=" + getID() + ",template_item_id="
                            + template.getID()));
            // temporarily turn off auth system, we have already checked the
            // permission on the top of the method
            // check it again will fail because we have already broken the
            // relation between the collection and the item
            ourContext.turnOffAuthorisationSystem();
            template.delete();
            ourContext.restoreAuthSystemState();
            template = null;
        }

        ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                getID(), "remove_template_item"));
    }

    /**
     * Add an item to the collection. This simply adds a relationship between
     * the item and the collection - it does nothing like set an issue date,
     * remove a personal workspace item etc. This has instant effect;
     * <code>update</code> need not be called.
     *
     * @param item
     *            item to add
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void addItem(Item item) throws SQLException, AuthorizeException
    {
        // Now that we've assumed only one target group per policy, the ADD group only
        // contains the users who can submit. We want to allow items to be added to the collection
        // by the submitter group as well as the curator/workflow groups as well. So, see if we
        // can authorize this activity by any of those groups before giving up.

        AuthorizeException ae = null;
        PolicyType[] authorizedAdders = { PolicyType.SUBMIT,
                PolicyType.WORKFLOW_STEP_1, PolicyType.WORKFLOW_STEP_2,
                PolicyType.WORKFLOW_STEP_3 };
        for (PolicyType policyType : authorizedAdders)
        {
            try {
                policyType.authorizeAction(ourContext, this);
                ae = null;
                break;
            } catch(AuthorizeException thrownAe) {
                ae = thrownAe;
            }
        }

        if (ae != null) {
            throw ae;
        }

        // Check authorisation
//        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_item", "collection_id="
                + getID() + ",item_id=" + item.getID()));

        // Create mapping
        TableRow row = DatabaseManager.row("collection2item");

        row.setColumn("collection_id", getID());
        row.setColumn("item_id", item.getID());

        DatabaseManager.insert(ourContext, row);

        ourContext.addEvent(new Event(Event.ADD, Constants.COLLECTION, getID(),
                Constants.ITEM, item.getID(), item.getHandle()));
    }

    /**
     * Remove an item. If the item is then orphaned, it is deleted.
     *
     * @param item
     *            item to remove
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeItem(Item item) throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        // will the item be an orphan?
        TableRow row = DatabaseManager
                .querySingle(
                        ourContext,
                        "SELECT COUNT(DISTINCT collection_id) AS num FROM collection2item WHERE item_id= ? ",
                        item.getID());

        DatabaseManager.setConstraintDeferred(ourContext, "coll2item_item_fk");
        if (row.getLongColumn("num") == 1)
        {
            // Orphan; delete it
            item.delete();
        }
        log.info(LogManager.getHeader(ourContext, "remove_item",
                "collection_id=" + getID() + ",item_id=" + item.getID()));

        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM collection2item WHERE collection_id= ? "
                        + "AND item_id= ? ", getID(), item.getID());
        DatabaseManager.setConstraintImmediate(ourContext, "coll2item_item_fk");

        ourContext.addEvent(new Event(Event.REMOVE, Constants.COLLECTION,
                getID(), Constants.ITEM, item.getID(), item.getHandle()));
    }

    /**
     * Update the collection metadata (including logo and workflow groups) to
     * the database. Inserts if this is a new collection.
     *
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation
        canEdit(true);

        log.info(LogManager.getHeader(ourContext, "update_collection",
                "collection_id=" + getID()));

        DatabaseManager.update(ourContext, collectionRow);

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                    getID(), null));
            modified = false;
        }
        if (modifiedMetadata)
        {
            ourContext.addEvent(new Event(Event.MODIFY_METADATA,
                    Constants.COLLECTION, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }
    }

    public boolean canEditBoolean() throws java.sql.SQLException
    {
        return canEditBoolean(true);
    }

    public boolean canEditBoolean(boolean useInheritance)
            throws java.sql.SQLException
    {
        try
        {
            canEdit(useInheritance);

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    public void canEdit() throws AuthorizeException, SQLException
    {
        canEdit(true);
    }

    public void canEdit(boolean useInheritance) throws AuthorizeException,
            SQLException
    {
        Community[] parents = getCommunities();

        for (int i = 0; i < parents.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.WRITE, useInheritance))
            {
                return;
            }

            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.ADD, useInheritance))
            {
                return;
            }
        }

        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE,
                useInheritance);
    }

    /**
     * Delete the collection, including the metadata and logo. Items that are
     * then orphans are deleted. Groups associated with this collection
     * (workflow participants and submitters) are NOT deleted.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    void delete() throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(ourContext, "delete_collection",
                "collection_id=" + getID()));

        ourContext.addEvent(new Event(Event.DELETE, Constants.COLLECTION,
                getID(), getHandle()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // remove subscriptions - hmm, should this be in Subscription.java?
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM subscription WHERE collection_id= ? ", getID());

        // Remove Template Item
        removeTemplateItem();

        // Remove items
        ItemIterator items = getAllItems();

        try
        {
            while (items.hasNext())
            {
                Item item = items.next();
                IndexBrowse ib = new IndexBrowse(ourContext);

                if (item.isOwningCollection(this))
                {
                    // the collection to be deleted is the owning collection,
                    // thus remove
                    // the item from all collections it belongs to
                    Collection[] collections = item.getCollections();
                    for (int i = 0; i < collections.length; i++)
                    {
                        // notify Browse of removing item.
                        ib.itemRemoved(item);
                        // Browse.itemRemoved(ourContext, itemId);
                        collections[i].removeItem(item);
                    }

                }
                // the item was only mapped to this collection, so just remove
                // it
                else
                {
                    // notify Browse of removing item mapping.
                    ib.indexItem(item);
                    // Browse.itemChanged(ourContext, item);
                    removeItem(item);
                }
            }
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new IOException(e.getMessage(), e);
        }
        finally
        {
            if (items != null)
            {
                items.close();
            }
        }

        // Delete bitstream logo
        setLogo(null);

        // Remove all authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        if (ConfigurationManager.getProperty("workflow", "workflow.framework")
                .equals("xmlworkflow"))
        {
            // Remove any xml_WorkflowItems
            XmlWorkflowItem[] xmlWfarray = XmlWorkflowItem.findByCollection(
                    ourContext, this);

            for (XmlWorkflowItem aXmlWfarray : xmlWfarray)
            {
                // remove the workflowitem first, then the item
                Item myItem = aXmlWfarray.getItem();
                aXmlWfarray.deleteWrapper();
                myItem.delete();
            }
        }
        else
        {
            // Remove any WorkflowItems
            WorkflowItem[] wfarray = WorkflowItem.findByCollection(ourContext,
                    this);

            for (WorkflowItem aWfarray : wfarray)
            {
                // remove the workflowitem first, then the item
                Item myItem = aWfarray.getItem();
                aWfarray.deleteWrapper();
                myItem.delete();
            }
        }

        // Remove any WorkspaceItems
        WorkspaceItem[] wsarray = WorkspaceItem.findByCollection(ourContext,
                this);

        for (WorkspaceItem aWsarray : wsarray)
        {
            aWsarray.deleteAll();
        }

        // get rid of the content count cache if it exists
        try
        {
            ItemCounter ic = new ItemCounter(ourContext);
            ic.remove(this);
        }
        catch (ItemCountException e)
        {
            // FIXME: upside down exception handling due to lack of good
            // exception framework
            throw new IllegalStateException(e.getMessage(), e);
        }

        // Remove any Handle
        HandleManager.unbindHandle(ourContext, this);

        if (ConfigurationManager.getProperty("workflow", "workflow.framework")
                .equals("xmlworkflow"))
        {
            // delete all CollectionRoles for this Collection
            for (CollectionRole collectionRole : CollectionRole
                    .findByCollection(ourContext, this.getID()))
            {
                collectionRole.delete();
            }
        }

        // Remove Configuration items for this Collection
        GlobusConfigurationManager.removeConfigForDso(ourContext, this);

        // Delete collection row
        DatabaseManager.delete(ourContext, collectionRow);

        // Remove any workflow groups - must happen after deleting collection
        Group g = null;

        g = getWorkflowGroup(1);

        if (g != null)
        {
            g.delete();
        }

        g = getWorkflowGroup(2);

        if (g != null)
        {
            g.delete();
        }

        g = getWorkflowGroup(3);

        if (g != null)
        {
            g.delete();
        }

        // Remove default administrators group
        g = getAdministrators();

        if (g != null)
        {
            g.delete();
        }

        // Remove default submitters group
        g = getSubmitters();

        if (g != null)
        {
            g.delete();
        }

    }

    /**
     * Get the communities this collection appears in
     *
     * @return array of <code>Community</code> objects
     * @throws SQLException
     */
    public Community[] getCommunities() throws SQLException
    {
        // Get the bundle table rows
        TableRowIterator tri = DatabaseManager
                .queryTable(
                        ourContext,
                        "community",
                        "SELECT community.* FROM community, community2collection WHERE "
                                + "community.community_id=community2collection.community_id "
                                + "AND community2collection.collection_id= ? ",
                        getID());

        // Build a list of Community objects
        List<Community> communities = new ArrayList<Community>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Community owner = (Community) ourContext.fromCache(
                        Community.class, row.getIntColumn("community_id"));

                if (owner == null)
                {
                    owner = new Community(ourContext, row);
                }

                communities.add(owner);

                // now add any parent communities
                Community[] parents = owner.getAllParents();
                communities.addAll(Arrays.asList(parents));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Collection as
     * this object, <code>false</code> otherwise
     *
     * @param other
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         collection as this object
     */
    @Override
    public boolean equals(Object other)
    {
        if (other == null)
        {
            return false;
        }
        if (getClass() != other.getClass())
        {
            return false;
        }
        final Collection otherCollection = (Collection) other;
        if (this.getID() != otherCollection.getID())
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 89
                * hash
                + (this.collectionRow != null ? this.collectionRow.hashCode()
                        : 0);
        return hash;
    }

    /**
     * Utility method for reading in a group from a group ID in a column. If the
     * column is null, null is returned.
     *
     * @param col
     *            the column name to read
     * @return the group referred to by that column, or null
     * @throws SQLException
     */
    private Group groupFromColumn(String col) throws SQLException
    {
        if (collectionRow.isColumnNull(col))
        {
            return null;
        }

        return Group.find(ourContext, collectionRow.getIntColumn(col));
    }

    /**
     * return type found in Constants
     *
     * @return int Constants.COLLECTION
     */
    public int getType()
    {
        return Constants.COLLECTION;
    }

    /**
     * return an array of collections that user has a given permission on
     * (useful for trimming 'select to collection' list) or figuring out which
     * collections a person is an editor for.
     *
     * @param context
     * @param comm
     *            (optional) restrict search to a community, else null
     * @param actionID
     *            of the action
     *
     * @return Collection [] of collections with matching permissions
     * @throws SQLException
     */
    public static Collection[] findAuthorized(Context context, Community comm,
            int actionID) throws java.sql.SQLException
    {
        List<Collection> myResults = new ArrayList<Collection>();

        Collection[] myCollections = null;

        if (comm != null)
        {
            myCollections = comm.getCollections();
        }
        else
        {
            myCollections = Collection.findAll(context);
        }

        // now build a list of collections you have authorization for
        for (int i = 0; i < myCollections.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(context,
                    myCollections[i], actionID))
            {
                myResults.add(myCollections[i]);
            }
        }

        myCollections = new Collection[myResults.size()];
        myCollections = (Collection[]) myResults.toArray(myCollections);

        return myCollections;
    }

    /**
     * counts items in this collection
     *
     * @return total items
     */
    public int countItems() throws SQLException
    {
        int itemcount = 0;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            String query = "SELECT count(*) FROM collection2item, item WHERE "
                    + "collection2item.collection_id =  ? "
                    + "AND collection2item.item_id = item.item_id "
                    + "AND in_archive ='1' AND item.withdrawn='0' ";

            statement = ourContext.getDBConnection().prepareStatement(query);
            statement.setInt(1, getID());

            rs = statement.executeQuery();
            if (rs != null)
            {
                rs.next();
                itemcount = rs.getInt(1);
            }
        }
        finally
        {
            if (rs != null)
            {
                try
                {
                    rs.close();
                }
                catch (SQLException sqle)
                {
                }
            }

            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException sqle)
                {
                }
            }
        }

        return itemcount;
    }

    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        DSpaceObject adminObject = null;
        Community community = null;
        Community[] communities = getCommunities();
        if (communities != null && communities.length > 0)
        {
            community = communities[0];
        }

        switch (action)
        {
        case Constants.REMOVE:
            if (AuthorizeConfiguration.canCollectionAdminPerformItemDeletion())
            {
                adminObject = this;
            }
            else if (AuthorizeConfiguration
                    .canCommunityAdminPerformItemDeletion())
            {
                adminObject = community;
            }
            break;

        case Constants.DELETE:
            if (AuthorizeConfiguration
                    .canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = community;
            }
            break;
        default:
            adminObject = this;
            break;
        }
        return adminObject;
    }

    @Override
    public DSpaceObject getParentObject() throws SQLException
    {
        Community[] communities = this.getCommunities();
        if (communities != null
                && (communities.length > 0 && communities[0] != null))
        {
            return communities[0];
        }
        else
        {
            return null;
        }
    }

    @Override
    public void updateLastModified()
    {
        // Also fire a modified event since the collection HAS been modified
        ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                getID(), null));
    }

    @Override
    public String toString()
    {
        return "Collection: " + getName();
    }

}
