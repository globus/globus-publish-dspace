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
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.globus.Globus;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.globus.GlobusClient;
import org.globus.groups.GlobusGroupSummary;
import org.globus.groups.GroupMember;
import org.globus.groups.GroupsInterface.MembershipRole;
import org.globus.groups.GroupsInterface.MembershipStatus;

/**
 * Class representing a group of e-people.
 *
 * @author David Stuve
 * @version $Revision$
 */
public class Group extends DSpaceObject
{
	// findAll sortby types
	public static final int ID = 0; // sort by ID

	public static final int NAME = 1; // sort by NAME (default)

	public static final int ANON_GROUP_ID = 0;
	public static final int ADMIN_GROUP_ID = 1;

    /**
     * The name for the group considered to be the "Anonymous" group
     */
    public static final String ANONYMOUS_GROUP_NAME = "Anonymous";


    private static Group anonGroup = null;

    /** log4j logger */
    private static Logger log = Logger.getLogger(Group.class);

    /** Our context */
    private final Context myContext;

    /** The row in the table representing this object */
    private final TableRow myRow;

    /** lists of epeople and groups in the group */
    private final List<EPerson> epeople = new ArrayList<EPerson>();

    private final List<Group> groups = new ArrayList<Group>();

    /** lists that need to be written out again */
    private boolean epeopleChanged = false;

    private boolean groupsChanged = false;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /**
     * Construct a Group from a given context and tablerow
     *
     * @param context
     * @param row
     */
    Group(Context context, TableRow row) throws SQLException
    {
        myContext = context;
        myRow = row;
        
        // Cache ourselves
        context.cache(this, row.getIntColumn("eperson_group_id"));
        
        modifiedMetadata = false;
        clearDetails();
    }

    /**
     *
     * Globus added
     *
     */
    public static Set<Integer> getGlobusMemberGroupIDs(Context c, EPerson e) {
        //log.info("Getting globus group ids for person " + e.getNetid());
        Set<Integer> groupIDs = new HashSet<Integer>();
        
        if (e == null){
            log.warn("EPerson is null");
            return groupIDs;
        }
        // get all the globus groups for a user and map them to DSpace groups.
        // this is used to get the curators list
        try{
            Globus globus = Globus.getGlobusClientFromContext(c);
            if (globus == null) {
                return groupIDs;
            }
            
            GlobusClient gClient = globus.getClient();
            if (gClient == null) {
                return groupIDs;
            }
			
            GlobusGroupSummary [] groups = null;
            EnumSet<MembershipStatus> statuses = EnumSet.of(MembershipStatus.active);
            EnumSet<MembershipRole> roles = EnumSet.of(MembershipRole.admin, MembershipRole.manager, MembershipRole.member);
            
            groups  = gClient.getGroups(roles, statuses, true, true, null);

            for (GlobusGroupSummary group: groups){
                TableRow row2 = DatabaseManager.findByUnique(c, "epersongroup",
                                                             "name", Globus.getPrefixedGroupName(group.id, group.name));

                if (row2 != null){
                    groupIDs.add(row2.getIntColumn("eperson_group_id"));
                }
            }        
        } catch (Exception ex){
            log.error("Exception getting groups ", ex);
        }
        //log.info("Group IDs" + groupIDs);
        return groupIDs;
    }

    /**
     * Globus added
     *
     * @param c The context to use for getting current user
     * @param g The group (which is a Globus group) to inspect
     * @return The DSpace person ids who are members of the Globus group
     */
    public static Set<Integer> getGlobusMemberIDs(Context c, Group g)  {
        Set<Integer> epeopleIDs = new HashSet<Integer>();
        log.info("Globus Extension: Getting all members from Globus");
        
        if (g == null){
            log.warn("Group is null");
            return epeopleIDs;
        }

        if (Globus.isGlobusGroup(g.getName())){
            return epeopleIDs;
        }

        try{
            EPerson cur = c.getCurrentUser();
            Globus globus = Globus.getGlobusClientFromContext(c);
            if (globus == null) {
                return epeopleIDs;
            }
            GlobusClient gClient = globus.getClient();

            if (gClient != null && Globus.isGlobusGroup(g.getName())){
                String gName = Globus.getUnPrefixedGroupID(g.getName());
                java.util.Collection<GroupMember> members = null;
                EnumSet<MembershipStatus> statuses = EnumSet.of(MembershipStatus.active);
                EnumSet<MembershipRole> roles = EnumSet.of(MembershipRole.admin, MembershipRole.manager, MembershipRole.member);
                
                members  = gClient.getMembersForGroup(gName, roles, statuses, null);
		        
                for (GroupMember member: members){
                    EPerson add = EPerson.findByNetid(c, member.identityId);
                    if (add != null) {
                        epeopleIDs.add(add.getID());
                    }
                }
            }		
        } catch (Exception ex){
            log.error(
                      "Error getting Globus Members -- perhaps user isnt able to get them?",
                      ex);
        }
        //log.info("EPersons: " + epeopleIDs);
        return epeopleIDs;
    }


    /**
     * Populate Group with eperson and group objects
     *
     * @throws SQLException
     */
    public void loadData()
    {
        // only populate if not already populated
        if (!isDataLoaded)
            {
                // Pre-load members of the group from Globus. 
                // the curation tasks get curators by checking here
                try{
                    GlobusClient gClient = Globus.getPrivlegedClient();
                    if (gClient != null && Globus.isGlobusGroup(getName())){
                        String gName = Globus.getUnPrefixedGroupID(getName());
                        java.util.Collection<GroupMember> members = null;
                        EnumSet<MembershipStatus> statuses = EnumSet.of(MembershipStatus.active);
                        EnumSet<MembershipRole> roles = EnumSet.of(MembershipRole.admin, MembershipRole.manager, MembershipRole.member);
			
                        members  = gClient.getMembersForGroup(gName, roles, statuses, null);
					
                        for (GroupMember member: members){
                            EPerson add = EPerson.findByNetid(myContext, member.identityId);
                            if (add != null) {
                                epeople.add(add);
                            }
                        }
                    }
                } catch (Exception ex){
                    log.error(
                              "Error getting Globus Members -- perhaps user isnt able to get them?",
                              ex);
                }
			
                // naughty thing to do - swallowing SQL exception and throwing it as
                // a RuntimeException - a hack to avoid changing the API all over
                // the place
                try
                    {
                        // get epeople objects
                        TableRowIterator tri = DatabaseManager.queryTable(myContext,"eperson",
                                                                          "SELECT eperson.* FROM eperson, epersongroup2eperson WHERE " +
                                                                          "epersongroup2eperson.eperson_id=eperson.eperson_id AND " +
                                                                          "epersongroup2eperson.eperson_group_id= ?",
                                                                          myRow.getIntColumn("eperson_group_id"));

                        try {
                            while (tri.hasNext())
                                {
                                    TableRow r = tri.next();
                                    
                                    // First check the cache
                                    EPerson fromCache = (EPerson) myContext.fromCache(
                                                                                      EPerson.class, r.getIntColumn("eperson_id"));

                                    if (fromCache != null)
                                        {
                                            epeople.add(fromCache);
                                        }
                                    else
                                        {
                                            epeople.add(new EPerson(myContext, r));
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

                        // now get Group objects
                        tri = DatabaseManager.queryTable(myContext,"epersongroup",
                                                         "SELECT epersongroup.* FROM epersongroup, group2group WHERE " +
                                                         "group2group.child_id=epersongroup.eperson_group_id AND "+
                                                         "group2group.parent_id= ? ",
                                                         myRow.getIntColumn("eperson_group_id"));

                        try
                            {
                                while (tri.hasNext())
                                    {
                                        TableRow r = tri.next();

                                        // First check the cache
                                        Group fromCache = (Group) myContext.fromCache(Group.class,
                                                                                      r.getIntColumn("eperson_group_id"));

                                        if (fromCache != null)
                                            {
                                                groups.add(fromCache);
                                            }
                                        else
                                            {
                                                groups.add(new Group(myContext, r));
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

                    }
                catch (Exception e)
                    {
                        throw new IllegalStateException(e);
                    }
                isDataLoaded = true;
            }
    }

    /**
     * Create a new group
     *
     * @param context
     *            DSpace context object
     */
    public static Group create(Context context) throws SQLException,
                                                       AuthorizeException
    {
        // Globus Removed this we want to allow non-admin to "import" groups
        // which results in a group being created
        /*if (!AuthorizeManager.isAdmin(context))
          {
          throw new AuthorizeException(
          "You must be an admin to create an EPerson Group");
          }*/

        // Create a table row
        TableRow row = DatabaseManager.create(context, "epersongroup");

        Group g = new Group(context, row);

        log.info(LogManager.getHeader(context, "create_group", "group_id="
                                      + g.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.GROUP, g.getID(), null));

        return g;
    }

    /**
     * get the ID of the group object
     *
     * @return id
     */
    @Override
    public int getID()
    {
        return myRow.getIntColumn("eperson_group_id");
    }

    /**
     * get name of Globus group
     *
     * @return name
     */
    public String getGlobusName()
    {
        if (Globus.isGlobusGroup(getName())) {
            GlobusClient gClient = Globus.getPrivlegedClient();

            return Globus.getGlobusGroupName(gClient, getName());
        }

        return getName();
    }

    /**
     * Helper for introspecting group to see if it is the Anonymous group
     * @return {@code true} if this is the anonymous group, {@code false} otherwise.
     */
    public boolean isAnon()
    {
        return getID() == ANON_GROUP_ID;
    }

    /**
     * Helper for introspecting group to see if it is the Administrative group
     * @return {@code true} if this is the administrative group, {@code false} otherwise.
     */
    public boolean isAdmin()
    {
        return getID() == ADMIN_GROUP_ID;
    }

    /**
     * get name of group
     *
     * @return name
     */
    @Override
    public String getName()
    {
        return myRow.getStringColumn("name");
    }

    /**
     * set name of group
     *
     * @param name
     *            new group name
     */
    public void setName(String name)
    {
        myRow.setColumn("name", name);
        modifiedMetadata = true;
        addDetails("name");
    }

    /**
     * add an eperson member
     *
     * @param e
     *            eperson
     */
    public void addMember(EPerson e)
    {
        // allow users to be added to the admin group
        if (isAdmin())
            {
                loadData(); // make sure Group has data loaded

	        if (isMember(e))
                    {
                        return;
                    }

	        epeople.add(e);
	        epeopleChanged = true;

	        myContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
            }
        else {
            log.warn("Group add member disabled for normal groups");
        }
    }

    /**
     * add group to this group
     *
     * @param g
     */
    public void addMember(Group g)
    {
        // We need to enable this as Globus groups are added
        // to root groups that relate to the workflow process
        loadData(); // make sure Group has data loaded

        // don't add if it's already a member
        // and don't add itself
        if (isMember(g) || getID()==g.getID())
            {
                return;
            }

        groups.add(g);
        groupsChanged = true;

        myContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
    }

    /**
     * remove an eperson from a group
     *
     * @param e
     *            eperson
     */
    public void removeMember(EPerson e)
    {
        if (isAdmin())
            {
                loadData(); // make sure Group has data loaded

                if (epeople.remove(e))
                    {
                        epeopleChanged = true;
                        myContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
                    }
            }
        else {
            log.warn("Group remove member disabled for normal groups");
        }

    }

    /**
     * remove group from this group
     *
     * @param g
     */
    public void removeMember(Group g)
    {
        log.info("Remove member  " + g);
        loadData(); // make sure Group has data loaded

        if (groups.remove(g))
            {
                groupsChanged = true;
                myContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
            }
    }

    /**
     * check to see if an eperson is a direct member.
     * If the eperson is a member via a subgroup will be returned <code>false</code>
     *
     * @param e
     *            eperson to check membership
     */
    public boolean isMember(EPerson e)
    {
        // special, group 0 is anonymous
        if (isAnon())
            {
                return true;
            }

        loadData(); // make sure Group has data loaded

        return epeople.contains(e);
    }

    /**
     * Check to see if g is a direct group member.
     * If g is a subgroup via another group will be returned <code>false</code>
     *
     * @param g
     *            group to check
     */
    public boolean isMember(Group g)
    {
        loadData(); // make sure Group has data loaded
        
        return groups.contains(g);
    }

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method
     *
     * @param c
     *            context
     * @param groupid
     *            group ID to check
     */
    public static boolean isMember(Context c, int groupid) throws SQLException
	{
            // special, everyone is member of group 0 (anonymous)
            if (groupid == ANON_GROUP_ID)
		{
                    return true;
		}

            EPerson currentuser = c.getCurrentUser();

            return epersonInGroup(c, groupid, currentuser);
	}

    /**
     * Fast check to see if a given EPerson is a member of a Group.
     * Does database lookup without instantiating all of the EPerson objects and
     * is thus a static method.
     *
     * @param c current DSpace context.
     * @param eperson candidate to test for membership.
     * @param groupid group whose membership is to be tested.
     * @return true if {@link eperson} is a member of Group {@link groupid}.
     * @throws SQLException passed through
     */
    public static boolean isMember(Context c, EPerson eperson, int groupid)
            throws SQLException
    {
        // Every EPerson is a member of Anonymous
        if (groupid == 0)
        {
            return true;
        }

        return epersonInGroup(c, groupid, eperson);
    }

    /**
     * Get all of the groups that an eperson is a member of.
     *
     * @param c
     * @param e
     * @throws SQLException
     */
    public static Group[] allMemberGroups(Context c, EPerson e)
        throws SQLException
    {
        List<Group> groupList = new ArrayList<Group>();

        Set<Integer> myGroups = allMemberGroupIDs(c, e);
        // now convert those Integers to Groups
        Iterator<Integer> i = myGroups.iterator();

        while (i.hasNext())
            {
                groupList.add(Group.find(c, i.next().intValue()));
            }

        return groupList.toArray(new Group[groupList.size()]);
    }

    /**
     * get Set of Integers all of the group memberships for an eperson
     *
     * @param c
     * @param e
     * @return Set of Integer groupIDs
     * @throws SQLException
     */
    public static Set<Integer> allMemberGroupIDs(Context c, EPerson e)
        throws SQLException
    {
        Set<Integer> groupIDs = new HashSet<Integer>();

        if (e != null)
            {
                // two queries - first to get groups eperson is a member of
                // second query gets parent groups for groups eperson is a member of

                TableRowIterator tri = DatabaseManager.queryTable(c,
                                                                  "epersongroup2eperson",
                                                                  "SELECT * FROM epersongroup2eperson WHERE eperson_id= ?", e
                                                                  .getID());

                try
                    {
                        while (tri.hasNext())
                            {
                                TableRow row = tri.next();

                                int childID = row.getIntColumn("eperson_group_id");

                                groupIDs.add(Integer.valueOf(childID));
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
            }
        // Also need to get all "Special Groups" user is a member of!
        // Otherwise, you're ignoring the user's membership to these groups!
        // However, we only do this is we are looking up the special groups
        // of the current user, as we cannot look up the special groups
        // of a user who is not logged in.
        if ((c.getCurrentUser() == null) || (((c.getCurrentUser() != null) && (c.getCurrentUser().getID() == e.getID()))))
            {
                Group[] specialGroups = c.getSpecialGroups();
                for(Group special : specialGroups)
                    {
                        groupIDs.add(Integer.valueOf(special.getID()));
                    }
            }

        // all the users are members of the anonymous group
        groupIDs.add(Integer.valueOf(ANON_GROUP_ID));

        // GET ALL GROUPS FROM GLOBUS...
        if (e != null) {
            groupIDs.addAll(getGlobusMemberGroupIDs(c, e));
        }

        // We need the following check for the auth check
        // It will ask if a user is in a specific workflow step group
        // Globus groups are the children of this group


        // now we have all owning groups, also grab all parents of owning groups
        // yes, I know this could have been done as one big query and a union,
        // but doing the Oracle port taught me to keep to simple SQL!

        StringBuilder groupQuery = new StringBuilder();
        groupQuery.append("SELECT * FROM group2groupcache WHERE ");

        Iterator<Integer> i = groupIDs.iterator();

        // Build a list of query parameters
        Object[] parameters = new Object[groupIDs.size()];
        int idx = 0;
        while (i.hasNext())
            {
                int groupID = i.next().intValue();

                parameters[idx++] = Integer.valueOf(groupID);

                groupQuery.append("child_id= ? ");
                if (i.hasNext())
                    {
                        groupQuery.append(" OR ");
                    }
            }

        // was member of at least one group
        // NOTE: even through the query is built dynamically, all data is
        // separated into the parameters array.
        TableRowIterator tri = DatabaseManager.queryTable(c, "group2groupcache",
                                                          groupQuery.toString(),
                                                          parameters);

        try
            {
                while (tri.hasNext())
                    {
                        TableRow row = tri.next();

                        int parentID = row.getIntColumn("parent_id");

                        groupIDs.add(Integer.valueOf(parentID));
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

        return groupIDs;
    }


    /**
     * Get all of the epeople who are a member of the
     * specified group, or a member of a sub-group of the
     * specified group, etc.
     *
     * @param c
     *          DSpace context
     * @param g
     *          Group object
     * @return   Array of EPerson objects
     * @throws SQLException
     */
    public static EPerson[] allMembers(Context c, Group g)
        throws SQLException
    {
        List<EPerson> epersonList = new ArrayList<EPerson>();

        Set<Integer> myEpeople = allMemberIDs(c, g);
        // now convert those Integers to EPerson objects
        Iterator<Integer> i = myEpeople.iterator();

        while (i.hasNext())
            {
                epersonList.add(EPerson.find(c, i.next().intValue()));
            }

        return epersonList.toArray(new EPerson[epersonList.size()]);
    }

    /**
     * Get Set of all Integers all of the epeople
     * members for a group
     *
     * @param c
     *          DSpace context
     * @param g
     *          Group object
     * @return Set of Integer epersonIDs
     * @throws SQLException
     */
    public static Set<Integer> allMemberIDs(Context c, Group g)
        throws SQLException
    {
        // two queries - first to get all groups which are a member of this group
        // second query gets all members of each group in the first query
        Set<Integer> epeopleIDs = new HashSet<Integer>();

        epeopleIDs.addAll(getGlobusMemberIDs(c, g));
        // TODO We may also need to iterate through children groups to
        // see if they have other users. I.e. if we are looking at the top group we
        // need to traverse all the children Globus groups to get users


        // Get all groups which are a member of this group
        TableRowIterator tri = DatabaseManager.queryTable(c, "group2groupcache",
                                                          "SELECT * FROM group2groupcache WHERE parent_id= ? ",
                                                          g.getID());

        Set<Integer> groupIDs = new HashSet<Integer>();

        try
            {
                while (tri.hasNext())
                    {
                        TableRow row = tri.next();

                        int childID = row.getIntColumn("child_id");

                        groupIDs.add(Integer.valueOf(childID));
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

        // Go through all subgroups (knowing they can only be Globus groups
        // and add members to this list.
        Iterator<Integer> gps = groupIDs.iterator();

        while (gps.hasNext())
            {
                int groupID = gps.next().intValue();
                Group child = find(c, groupID);
                epeopleIDs.addAll(getGlobusMemberIDs(c, child));
            }

        log.info("EPeople: " + epeopleIDs);
        return epeopleIDs;
    }

    private static boolean epersonInGroup(Context c, int groupID, EPerson e)
        throws SQLException
    {
        Set<Integer> groupIDs = Group.allMemberGroupIDs(c, e);
        return groupIDs.contains(Integer.valueOf(groupID));
    }

    /**
     * find the group by its ID
     *
     * @param context
     * @param id
     */
    public static Group find(Context context, int id) throws SQLException
    {
        // First check the cache
        Group fromCache = (Group) context.fromCache(Group.class, id);

        if (fromCache != null)
            {
                return fromCache;
            }

        TableRow row = DatabaseManager.find(context, "epersongroup", id);

        if (row == null)
            {
                return null;
            }
        else
            {
                return new Group(context, row);
            }
    }

    public synchronized static Group findAnonGroup(Context context) throws SQLException
    {
        if (anonGroup == null) {
            anonGroup = find(context, ANON_GROUP_ID);
        }
        return anonGroup;
    }

    /**
     * Find the group by its name - assumes name is unique
     *
     * @param context
     * @param name
     *
     * @return the named Group, or null if not found
     */
    public static Group findByName(Context context, String name)
        throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "epersongroup",
                                                    "name", name);

        if (row == null)
            {
                return null;
            }
        else
            {
                // First check the cache
                Group fromCache = (Group) context.fromCache(Group.class, row
                                                            .getIntColumn("eperson_group_id"));

                if (fromCache != null)
                    {
                        return fromCache;
                    }
                else
                    {
                        return new Group(context, row);
                    }
            }
    }

    /**
     * Finds all groups in the site
     *
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- Group.ID or Group.NAME
     *
     * @return array of all groups in the site
     */
    public static Group[] findAll(Context context, int sortField)
        throws SQLException
    {
        String s;

        switch (sortField)
            {
            case ID:
                s = "eperson_group_id";

                break;

            case NAME:
                s = "name";
                
                break;

		default:
			s = "name";
		}

        // NOTE: The use of 's' in the order by clause can not cause an SQL
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.queryTable(
                                                           context, "epersongroup",
                                                           "SELECT * FROM epersongroup ORDER BY "+s);

        try
            {
                List<TableRow> gRows = rows.toList();

                Group[] groups = new Group[gRows.size()];

                for (int i = 0; i < gRows.size(); i++)
                    {
                        TableRow row = gRows.get(i);

                        // First check the cache
                        Group fromCache = (Group) context.fromCache(Group.class, row
                                                                    .getIntColumn("eperson_group_id"));

                        if (fromCache != null)
                            {
                                groups[i] = fromCache;
                            }
                        else
                            {
                                groups[i] = new Group(context, row);
                            }
                    }
                
                return groups;
            }
        finally
            {
                if (rows != null)
                    {
                        rows.close();
                    }
            }
    }


    /**
     * Find the groups that match the search query across eperson_group_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return array of Group objects
     */
    public static Group[] search(Context context, String query)
        throws SQLException
    {
        return search(context, query, -1, -1);
    }

    /**
     * Find the groups that match the search query across eperson_group_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset
     * @param limit
     *            Maximum number of matches returned
     *
     * @return array of Group objects
     */
    public static Group[] search(Context context, String query, int offset, int limit)
        throws SQLException
    {
        // This doesn't support Globus group search, may be inaccurate when
        // used with Globus, but it doesn't seem to be used in any significant way
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT * FROM epersongroup WHERE LOWER(name) LIKE LOWER(?) OR eperson_group_id = ? ORDER BY name ASC ");

        // Add offset and limit restrictions - Oracle requires special code
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            {
                // First prepare the query to generate row numbers
                if (limit > 0 || offset > 0)
                    {
                        queryBuf.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
                        queryBuf.append(") ");
                    }

                // Restrict the number of rows returned based on the limit
                if (limit > 0)
                    {
                        queryBuf.append("rec WHERE rownum<=? ");
                        // If we also have an offset, then convert the limit into the maximum row number
                        if (offset > 0)
                            {
                                limit += offset;
                            }
                    }

                // Return only the records after the specified offset (row number)
                if (offset > 0)
                    {
                        queryBuf.insert(0, "SELECT * FROM (");
                        queryBuf.append(") WHERE rnum>?");
                    }
            }
        else
            {
                if (limit > 0)
                    {
                        queryBuf.append(" LIMIT ? ");
                    }

                if (offset > 0)
                    {
                        queryBuf.append(" OFFSET ? ");
                    }
            }

        String dbquery = queryBuf.toString();

        // When checking against the eperson-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[]{params, int_param};
        if (limit > 0 && offset > 0)
            {
                paramArr = new Object[]{params, int_param, limit, offset};
            }
        else if (limit > 0)
            {
                paramArr = new Object[]{params, int_param, limit};
            }
        else if (offset > 0)
            {
                paramArr = new Object[]{params, int_param, offset};
            }

        TableRowIterator rows =
            DatabaseManager.query(context, dbquery, paramArr);

        try
            {
                List<TableRow> groupRows = rows.toList();
                Group[] groups = new Group[groupRows.size()];

                for (int i = 0; i < groupRows.size(); i++)
                    {
                        TableRow row = groupRows.get(i);

                        // First check the cache
                        Group fromCache = (Group) context.fromCache(Group.class, row
                                                                    .getIntColumn("eperson_group_id"));

                        if (fromCache != null)
                            {
                                groups[i] = fromCache;
                            }
                        else
                            {
                                groups[i] = new Group(context, row);
                            }
                    }
                return groups;
            }
        finally
            {
                if (rows != null)
                    {
                        rows.close();
                    }
            }
    }

    /**
     * Returns the total number of groups returned by a specific query, without the overhead
     * of creating the Group objects to store the results.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return the number of groups matching the query
     */
    // This method seems obosolete (no references), but note that it doesn't
    // work properly with Globus integration
    public static int searchResultCount(Context context, String query)
        throws SQLException
    {
        String params = "%"+query.toLowerCase()+"%";
        String dbquery = "SELECT count(*) as gcount FROM epersongroup WHERE LOWER(name) LIKE LOWER(?) OR eperson_group_id = ? ";

        // When checking against the eperson-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Get all the epeople that match the query
        TableRow row = DatabaseManager.querySingle(context, dbquery, new Object[] {params, int_param});

        // use getIntColumn for Oracle count data
        Long count;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            {
                count = Long.valueOf(row.getIntColumn("gcount"));
            }
        else  //getLongColumn works for postgres
            {
                count = Long.valueOf(row.getLongColumn("gcount"));
            }

        return count.intValue();
    }


    /**
     * Delete a group
     *
     */
    public void delete() throws SQLException
    {
        if (isAnon() || isAdmin()) {
            log.error("Attempt to delete Group with id " + getID() + " which is preotected");
            return;
        }

        // FIXME: authorizations

        myContext.addEvent(new Event(Event.DELETE, Constants.GROUP, getID(), getName()));

        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove any ResourcePolicies that reference this group
        AuthorizeManager.removeGroupPolicies(myContext, getID());

        // Remove any group memberships first
        DatabaseManager.updateQuery(myContext,
                                    "DELETE FROM EPersonGroup2EPerson WHERE eperson_group_id= ? ",
                                    getID());

        // remove any group2groupcache entries
        DatabaseManager.updateQuery(myContext,
                                    "DELETE FROM group2groupcache WHERE parent_id= ? OR child_id= ? ",
                                    getID(),getID());

        // Now remove any group2group assignments
        DatabaseManager.updateQuery(myContext,
                                    "DELETE FROM group2group WHERE parent_id= ? OR child_id= ? ",
                                    getID(),getID());

        // don't forget the new table
        deleteEpersonGroup2WorkspaceItem();

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        epeople.clear();

        log.info(LogManager.getHeader(myContext, "delete_group", "group_id="
                                      + getID()));
    }

    /**
     * @throws SQLException
     */
    private void deleteEpersonGroup2WorkspaceItem() throws SQLException
    {
        DatabaseManager.updateQuery(myContext,
                                    "DELETE FROM EPersonGroup2WorkspaceItem WHERE eperson_group_id= ? ",
                                    getID());
    }

    /**
     * Return EPerson members of a Group
     */
    public EPerson[] getMembers()
	{
            loadData(); // make sure all data is loaded

            EPerson[] myArray = new EPerson[epeople.size()];
            myArray = epeople.toArray(myArray);

            return myArray;
	}

    /**
     * Return Group members of a Group.
     */
    public Group[] getMemberGroups()
    {
        loadData(); // make sure all data is loaded

        Group[] myArray = new Group[groups.size()];
        myArray = groups.toArray(myArray);

        return myArray;
    }

    /**
     * Return true if group has no direct or indirect members
     */
    public boolean isEmpty()
    {
        loadData(); // make sure all data is loaded

        if (Globus.isGlobusGroup(this.getName())){
            // assume all Globus groups have users (without actually checking)
            return false;
        }

        // the only fast check available is on epeople...
        boolean hasMembers = epeople.size() != 0;

        if (hasMembers)
            {
                return false;
            }
        else
            {
                // well, groups is never null...
                for (Group subGroup : groups){
                    hasMembers = !subGroup.isEmpty();
                    if (hasMembers){
                        return false;
                    }
                }
                return !hasMembers;
            }
    }

    /**
     * Update the group - writing out group object and EPerson list if necessary
     */
    @Override
    public void update() throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation
        DatabaseManager.update(myContext, myRow);

        if (modifiedMetadata)
            {
                myContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.GROUP, getID(), getDetails()));
                modifiedMetadata = false;
                clearDetails();
            }

        // Redo eperson mappings if they've changed
        if (epeopleChanged)
            {
                // Remove any existing mappings
                DatabaseManager.updateQuery(myContext,
                                            "delete from epersongroup2eperson where eperson_group_id= ? ",
                                            getID());

                // Add new mappings
                Iterator<EPerson> i = epeople.iterator();

                while (i.hasNext())
                    {
                        EPerson e = i.next();

                        TableRow mappingRow = DatabaseManager.row("epersongroup2eperson");
                        mappingRow.setColumn("eperson_id", e.getID());
                        mappingRow.setColumn("eperson_group_id", getID());
                        DatabaseManager.insert(myContext, mappingRow);
                    }

                epeopleChanged = false;
            }

        // Redo Group mappings if they've changed
        if (groupsChanged)
            {
                // Remove any existing mappings
                DatabaseManager.updateQuery(myContext,
                                            "delete from group2group where parent_id= ? ",
                                            getID());

                // Add new mappings
                Iterator<Group> i = groups.iterator();

                while (i.hasNext())
                    {
                        Group g = i.next();

                        TableRow mappingRow = DatabaseManager.row("group2group");
                        mappingRow.setColumn("parent_id", getID());
                        mappingRow.setColumn("child_id", g.getID());
                        DatabaseManager.insert(myContext, mappingRow);
                    }

                // groups changed, now change group cache
                rethinkGroupCache();

                groupsChanged = false;
            }

        log.info(LogManager.getHeader(myContext, "update_group", "group_id="
                                      + getID()));
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Group as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same group
     *         as this object
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            {
                return false;
            }
        if (getClass() != obj.getClass())
            {
                return false;
            }
        final Group other = (Group) obj;
        if(getID() != other.getID())
            {
                return false;
            }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + (myRow != null ? myRow.hashCode() : 0);
        return hash;
    }



    @Override
    public int getType()
    {
        return Constants.GROUP;
    }

    @Override
    public String getHandle()
    {
        return null;
    }

    /**
     * Regenerate the group cache AKA the group2groupcache table in the database -
     * meant to be called when a group is added or removed from another group
     *
     */
    private void rethinkGroupCache() throws SQLException
    {
        // read in the group2group table
        TableRowIterator tri = DatabaseManager.queryTable(myContext, "group2group",
                                                          "SELECT * FROM group2group");

        Map<Integer,Set<Integer>> parents = new HashMap<Integer,Set<Integer>>();

        try
            {
                while (tri.hasNext())
                    {
                        TableRow row = tri.next();

                        Integer parentID = Integer.valueOf(row.getIntColumn("parent_id"));
                        Integer childID = Integer.valueOf(row.getIntColumn("child_id"));

                        // if parent doesn't have an entry, create one
                        if (!parents.containsKey(parentID))
                            {
                                Set<Integer> children = new HashSet<Integer>();

                                // add child id to the list
                                children.add(childID);
                                parents.put(parentID, children);
                            }
                        else
                            {
                                // parent has an entry, now add the child to the parent's record
                                // of children
                                Set<Integer> children =  parents.get(parentID);
                                children.add(childID);
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

        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash
        for (Map.Entry<Integer, Set<Integer>> parent : parents.entrySet())
            {
                Set<Integer> myChildren = getChildren(parents, parent.getKey());
                parent.getValue().addAll(myChildren);
            }

        // empty out group2groupcache table
        DatabaseManager.updateQuery(myContext,
                                    "DELETE FROM group2groupcache WHERE id >= 0");

        // write out new one
        for (Map.Entry<Integer, Set<Integer>> parent : parents.entrySet())
            {
                int parentID = parent.getKey().intValue();

                for (Integer child : parent.getValue())
                    {
                        TableRow row = DatabaseManager.row("group2groupcache");

                        row.setColumn("parent_id", parentID);
                        row.setColumn("child_id", child);

                        DatabaseManager.insert(myContext, row);
                    }
            }
    }

    /**
     * Used recursively to generate a map of ALL of the children of the given
     * parent
     *
     * @param parents
     *            Map of parent,child relationships
     * @param parent
     *            the parent you're interested in
     * @return Map whose keys are all of the children of a parent
     */
    private Set<Integer> getChildren(Map<Integer,Set<Integer>> parents, Integer parent)
    {
        Set<Integer> myChildren = new HashSet<Integer>();

        // degenerate case, this parent has no children
        if (!parents.containsKey(parent))
            {
                return myChildren;
            }

        // got this far, so we must have children
        Set<Integer> children =  parents.get(parent);

        // now iterate over all of the children
        Iterator<Integer> i = children.iterator();

        while (i.hasNext())
            {
                Integer childID = i.next();

                // add this child's ID to our return set
                myChildren.add(childID);

                // and now its children
                myChildren.addAll(getChildren(parents, childID));
            }

        return myChildren;
    }

    @Override
    public DSpaceObject getParentObject() throws SQLException
    {
        // could a collection/community administrator manage related groups?
        // check before the configuration options could give a performance gain
        // if all group management are disallowed
        if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup()
            || AuthorizeConfiguration.canCollectionAdminManageSubmitters()
            || AuthorizeConfiguration.canCollectionAdminManageWorkflows()
            || AuthorizeConfiguration.canCommunityAdminManageAdminGroup()
            || AuthorizeConfiguration
            .canCommunityAdminManageCollectionAdminGroup()
            || AuthorizeConfiguration
            .canCommunityAdminManageCollectionSubmitters()
            || AuthorizeConfiguration
            .canCommunityAdminManageCollectionWorkflows())
            {
                // is this a collection related group?
                TableRow qResult = DatabaseManager
                    .querySingle(
                                 myContext,
                                 "SELECT collection_id, workflow_step_1, workflow_step_2, " +
                                 " workflow_step_3, submitter, admin FROM collection "
                                 + " WHERE workflow_step_1 = ? OR "
                                 + " workflow_step_2 = ? OR "
                                 + " workflow_step_3 = ? OR "
                                 + " submitter =  ? OR " + " admin = ?",
                                 getID(), getID(), getID(), getID(), getID());
                if (qResult != null)
                    {
                        Collection collection = Collection.find(myContext, qResult
								.getIntColumn("collection_id"));

                        if (qResult.getIntColumn("workflow_step_1") == getID() ||
                            qResult.getIntColumn("workflow_step_2") == getID() ||
                            qResult.getIntColumn("workflow_step_3") == getID())
                            {
                                if (AuthorizeConfiguration.canCollectionAdminManageWorkflows())
                                    {
                                        return collection;
                                    }
                                else if (AuthorizeConfiguration.canCommunityAdminManageCollectionWorkflows())
                                    {
                                        return collection.getParentObject();
                                    }
                            }
                        if (qResult.getIntColumn("submitter") == getID())
                            {
                                if (AuthorizeConfiguration.canCollectionAdminManageSubmitters())
                                    {
                                        return collection;
                                    }
                                else if (AuthorizeConfiguration.canCommunityAdminManageCollectionSubmitters())
                                    {
                                        return collection.getParentObject();
                                    }
                            }
                        if (qResult.getIntColumn("admin") == getID())
                            {
                                if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup())
                                    {
                                        return collection;
                                    }
                                else if (AuthorizeConfiguration.canCommunityAdminManageCollectionAdminGroup())
                                    {
                                        return collection.getParentObject();
                                    }
                            }
                    }
                // is the group related to a community and community administrator allowed
                // to manage it?
                else if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup())
                    {
                        qResult = DatabaseManager.querySingle(myContext,
                                                              "SELECT community_id FROM community "
                                                              + "WHERE admin = ?", getID());

                        if (qResult != null)
                            {
                                Community community = Community.find(myContext, qResult
                                                                     .getIntColumn("community_id"));
                                return community;
                            }
                    }
            }
        return null;
    }

    @Override
    public void updateLastModified()
    {

    }

    @Override
    public String toString()
    {
        return "Group: " + getName() + " :" + ((this.groups != null) ? this.groups.toString() : "");
    }
}
