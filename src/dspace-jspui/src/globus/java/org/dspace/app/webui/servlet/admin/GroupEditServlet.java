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
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.globus.Globus;
import org.dspace.globus.GlobusWebAppIntegration;
import org.dspace.handle.HandleManager;

/**
 * Servlet for editing groups
 *
 * @author dstuve
 * @version $Revision$
 */
public class GroupEditServlet extends DSpaceServlet
{
	/** log4j logger */
    private static Logger log = Logger
            .getLogger(GroupEditServlet.class);

    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSPost(c, request, response);
    }

    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        // Find out if there's a group parameter
        int groupID = UIUtil.getIntParameter(request, "group_id");
        Group group = null;
        log.info("Group edit for " + groupID);

        // check if this is a Globus selection callback
        boolean globusCallback = (request.getParameter("globusCallback") != null);

        // see if this is coming from a community or collection link
        String collectionHandle = request.getParameter("collection_handle");
        String communityHandle = request.getParameter("community_handle");

        // added to skip this page
        boolean skipGroupsPage = (request.getParameter("skipGroupsPage") != null);

        String redirectURL = request.getRequestURL().toString();
        if (redirectURL.endsWith(".jsp")){
            redirectURL = redirectURL.substring(0, redirectURL.lastIndexOf('.'));
        }

        if (groupID >= 0)
        {
            group = Group.find(c, groupID);
        }
        // group is set
        if (group != null)
        {
            // is this user authorized to edit this group?
            AuthorizeManager.authorizeAction(c, group, Constants.ADD);

            boolean submit_edit = (request.getParameter("submit_edit") != null);
            boolean submit_group_update = (request.getParameter("submit_group_update") != null);
            boolean submit_group_delete = (request.getParameter("submit_group_delete") != null);
            boolean submit_confirm_delete = (request.getParameter("submit_confirm_delete") != null);
            boolean submit_cancel_delete = (request.getParameter("submit_cancel_delete") != null);

            if (globusCallback && !submit_group_update && !submit_group_delete){
            	// If its a Globus callback create the new placeholder Globus group
            	// and add to members

				// We only allow a single group to be selected
				String [] globusGroupIds = request.getParameterValues("groupUuid[0]");
				String [] globusGroupNames = request.getParameterValues("groupName[0]");

				log.info("Globus groups selected: "+ globusGroupIds);
				// if its a globus callback we need to set current group members

				if (globusGroupIds != null){
					Set<Integer>memberSet = new HashSet<Integer>();
	                Group[] membergroups = group.getMemberGroups();
	                log.info("Member groups: " + membergroups);

	                // Delete existing members (we only allow a single globus group child)
	                // process members, removing any that aren't in eperson_ids
                    for (int x = 0; x < membergroups.length; x++){
                        Group g = membergroups[x];
                        group.removeMember(g);
                    }

					for (int i=0; i < globusGroupIds.length; i++){
						try{
							String globusGroupName = Globus.getPrefixedGroupName(globusGroupIds[i], globusGroupNames[i]);
							Group newGroup = Group.findByName(c, globusGroupName);
							if (newGroup == null){
								newGroup = Group.create(c);
					          	newGroup.setName(globusGroupName);
				        	  	newGroup.update();
							}
							group.addMember(Group.find(c, newGroup.getID()));
							memberSet.add(newGroup.getID());
						}catch (Exception ex){
							log.error("Error adding Globus Group " + globusGroupIds[i]);
						}
					}
				}
				group.update();
				request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());
                request.setAttribute("collection_handle", collectionHandle);
                request.setAttribute("community_handle", communityHandle);

                if (skipGroupsPage){
                	String queryString = "";
                    Collection collection = null;
                    if (collectionHandle != null) {
                        collection = (Collection) HandleManager.resolveToObject(c, collectionHandle);
                        if (collection != null) {
                            queryString += "?action=" + EditCommunitiesServlet.START_EDIT_COLLECTION +
                            		"&collection_id=" + collection.getID();
                        }
                    }

                    Community community = null;
                    if (communityHandle != null) {

                        community = (Community) HandleManager.resolveToObject(c, communityHandle);
                        if (community != null) {
                            queryString += "?action=" + EditCommunitiesServlet.START_EDIT_COMMUNITY +
                            "&community_id=" + community.getID();
                        }
                    }
                    response.sendRedirect(request.getContextPath() + "/tools/edit-communities" + queryString);

                } else {
                    JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
                }
                c.complete();
            // just chosen a group to edit - get group and pass it to
            // group-edit.jsp
            }else if (submit_edit && !submit_group_update && !submit_group_delete)
            {
                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());
                request.setAttribute("collection_handle", collectionHandle);
                request.setAttribute("community_handle", communityHandle);

                if (skipGroupsPage){
                    response.sendRedirect(GlobusWebAppIntegration.
                                          getGroupSelectionUrl(groupID,
                                                               redirectURL,
                                                               collectionHandle,
                                                               communityHandle,
                                                               true));
                } else {
                    JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
                }
            }
            // update the members of the group
            else if (submit_group_update)
            {
                // first off, did we change the group name?
                String newName = request.getParameter("group_name");

	            if (newName != null && !newName.equals(group.getName()))
	            {
	            	group.setName(newName);
	            	group.update();
	            }

                int[] eperson_ids = UIUtil.getIntParameters(request,
                        "eperson_id");

				int[] group_ids = UIUtil.getIntParameters(request, "group_ids");


                // now get members, and add new ones and remove missing ones
                EPerson[] members = group.getMembers();
                Group[] membergroups = group.getMemberGroups();

                if (eperson_ids != null)
                {
                    // some epeople were listed, now make group's epeople match
                    // given epeople
                    Set memberSet = new HashSet();
                    Set epersonIDSet = new HashSet();

                    // add all members to a set
                    for (int x = 0; x < members.length; x++)
                    {
                        Integer epersonID = Integer.valueOf(members[x].getID());
                        memberSet.add(epersonID);
                    }

                    // now all eperson_ids are put in a set
                    for (int x = 0; x < eperson_ids.length; x++)
                    {
                        epersonIDSet.add(Integer.valueOf(eperson_ids[x]));
                    }

                    // process eperson_ids, adding those to group not already
                    // members
                    Iterator i = epersonIDSet.iterator();

                    while (i.hasNext())
                    {
                        Integer currentID = (Integer) i.next();

                        if (!memberSet.contains(currentID))
                        {
                            group.addMember(EPerson.find(c, currentID
                                    .intValue()));
                        }
                    }

                    // process members, removing any that aren't in eperson_ids
                    for (int x = 0; x < members.length; x++)
                    {
                        EPerson e = members[x];

                        if (!epersonIDSet.contains(Integer.valueOf(e.getID())))
                        {
                            group.removeMember(e);
                        }
                    }
                }
                else
                {
                    // no members found (ids == null), remove them all!

                    for (int y = 0; y < members.length; y++)
                    {
                        group.removeMember(members[y]);
                    }
                }

                if (group_ids != null)
                {
                    // some groups were listed, now make group's member groups
                    // match given group IDs
                    Set memberSet = new HashSet();
                    Set groupIDSet = new HashSet();

                    // add all members to a set
                    for (int x = 0; x < membergroups.length; x++)
                    {
                        Integer myID = Integer.valueOf(membergroups[x].getID());
                        memberSet.add(myID);
                    }

                    // now all eperson_ids are put in a set
                    for (int x = 0; x < group_ids.length; x++)
                    {
                        groupIDSet.add(Integer.valueOf(group_ids[x]));
                    }

                    // process group_ids, adding those to group not already
                    // members
                    Iterator i = groupIDSet.iterator();

                    while (i.hasNext())
                    {
                        Integer currentID = (Integer) i.next();

                        if (!memberSet.contains(currentID))
                        {
                            group
                                    .addMember(Group.find(c, currentID
                                            .intValue()));
                        }
                    }

                    // process members, removing any that aren't in eperson_ids
                    for (int x = 0; x < membergroups.length; x++)
                    {
                        Group g = membergroups[x];

                        if (!groupIDSet.contains(Integer.valueOf(g.getID())))
                        {
                            group.removeMember(g);
                        }
                    }

                }
                else
                {
                    // no members found (ids == null), remove them all!
                    for (int y = 0; y < membergroups.length; y++)
                    {
                        group.removeMember(membergroups[y]);
                    }
                }

                group.update();

                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());
                request.setAttribute("collection_handle", collectionHandle);
                request.setAttribute("community_handle", communityHandle);

                JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
                c.complete();
            }
            else if (submit_group_delete)
            {
                // direct to a confirmation step
                request.setAttribute("group", group);
                JSPManager.showJSP(request, response, "/dspace-admin/group-confirm-delete.jsp");
            }
            else if (submit_confirm_delete)
            {
                // phony authorize, only admins can do this
                AuthorizeManager.authorizeAction(c, group, Constants.WRITE);

                // delete group, return to group-list.jsp
                group.delete();

                showMainPage(c, request, response);
            }
            else if (submit_cancel_delete)
            {
                // show group list
                showMainPage(c, request, response);
            }
            else
            {
                // unknown action, show edit page
                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());
                request.setAttribute("collection_handle", collectionHandle);
                request.setAttribute("community_handle", communityHandle);

                if (skipGroupsPage){
                    response.sendRedirect(GlobusWebAppIntegration.
                                          getGroupSelectionUrl(groupID,
                                                               redirectURL,
                                                               collectionHandle,
                                                               communityHandle,
                                                               true));
                } else {
                    JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
                }
                //JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
            }
        }
        else
        // no group set
        {
            // want to add a group - create a blank one, and pass to
            // group_edit.jsp
            String button = UIUtil.getSubmitButton(request, "submit");

            if (button.equals("submit_add"))
            {
                group = Group.create(c);

                group.setName("new group" + group.getID());
                group.update();

                request.setAttribute("group", group);
                request.setAttribute("members", group.getMembers());
                request.setAttribute("membergroups", group.getMemberGroups());
                request.setAttribute("collection_handle", collectionHandle);
                request.setAttribute("community_handle", communityHandle);

                JSPManager.showJSP(request, response, "/tools/group-edit.jsp");
                c.complete();
            }
            else
            {
                // show the main page (select groups)
                showMainPage(c, request, response);
            }
        }
    }

    private void showMainPage(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        Group[] groups = Group.findAll(c, Group.NAME);

        // if( groups == null ) { System.out.println("groups are null"); }
        // else System.out.println("# of groups: " + groups.length);
        request.setAttribute("groups", groups);

        JSPManager.showJSP(request, response, "/tools/group-list.jsp");
        c.complete();
    }
}
