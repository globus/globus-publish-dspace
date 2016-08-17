<%--
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
--%>


<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show form allowing edit of community metadata
  -
  - Attributes:
  -    community   - community to edit, if editing an existing one.  If this
  -                  is null, we are creating one.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.globus.Globus" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
	Context context = UIUtil.obtainContext(request);

    Community community = (Community) request.getAttribute("community");
    int parentID = UIUtil.getIntParameter(request, "parent_community_id");
    // Is the logged in user a sys admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    Boolean adminCreateGroup = (Boolean)request.getAttribute("admin_create_button");
    boolean bAdminCreateGroup = (adminCreateGroup == null ? false : adminCreateGroup.booleanValue());

    Boolean adminRemoveGroup = (Boolean)request.getAttribute("admin_remove_button");
    boolean bAdminRemoveGroup = (adminRemoveGroup == null ? false : adminRemoveGroup.booleanValue());

    Boolean policy = (Boolean)request.getAttribute("policy_button");
    boolean bPolicy = (policy == null ? false : policy.booleanValue());

    Boolean delete = (Boolean)request.getAttribute("delete_button");
    boolean bDelete = (delete == null ? false : delete.booleanValue());

    Boolean adminCommunity = (Boolean)request.getAttribute("admin_community");
    boolean bAdminCommunity = (adminCommunity == null ? false : adminCommunity.booleanValue());
    String name = "";
    String shortDesc = "";
    String intro = "";
    String copy = "";
    String side = "";
    Group admins = null;

    Bitstream logo = null;
    String adminGroupsLink = "";

    if (community != null)
    {
        name = community.getMetadata("name");
        shortDesc = community.getMetadata("short_description");
        intro = community.getMetadata("introductory_text");
        copy = community.getMetadata("copyright_text");
        side = community.getMetadata("side_bar_text");
        logo = community.getLogo();
        admins = community.getAdministrators();
        if (admins != null){
        	Group[] adminMembers = admins.getMemberGroups();
	    	if (adminMembers != null && adminMembers.length >= 1){
	    		adminGroupsLink = Globus.getGlobusGroupLink(adminMembers[0].getName(), 20);
	    	}
        }
    }
%>

<dspace:layout style="submission" titlekey="jsp.tools.edit-community.title"
		       locbar="link"
		       parentlink="/dspace-admin"
		       parenttitlekey="jsp.administer" nocache="true">

<div class="row">
<%
    if (community == null)
    {
%>
    <h3 class="col-md-12"><fmt:message key="jsp.tools.edit-community.heading1"/>
    	<span>
        	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#editcommunity\"%>"><fmt:message key="jsp.help"/></dspace:popup>
        </span>
    </h3>
<%
    }
    else
    {
%>
    <h3 class="col-md-8"><fmt:message key="jsp.tools.edit-community.heading2">
        <fmt:param><%= (name != null ? name : community.getHandle()) %></fmt:param>
        </fmt:message>
        <span>
        	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#editcommunity\"%>"><fmt:message key="jsp.help"/></dspace:popup>
        </span>
    </h3>
    <% if(bDelete) { %>
              <form class="col-md-4" method="post" action="">
                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COMMUNITY %>" />
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input class="col-md-12 btn btn-danger" type="submit" name="submit_delete" value="<fmt:message key="jsp.tools.edit-community.button.delete"/>" />
              </form>
    <% } %>
<%
    }
%>
</div>

<form method="post" action="<%= request.getContextPath() %>/tools/edit-communities">
<div class="row">
	<div class="col-md-<%= community != null?"8":"12" %>">
	<div class="panel panel-primary">
		<div class="panel-heading"><fmt:message key="jsp.tools.edit-community.form.basic-metadata"/></div>

        <div class="panel-body">
<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
            <div class="row">
                <label for="name" class="col-md-3"><fmt:message key="jsp.tools.edit-community.form.label1"/></label>
                <span class="col-md-9"><input class="form-control" type="text" name="name" value="<%= Utils.addEntities(name) %>" size="50" /></span>
            </div><br/>
            <div class="row">
                <label for="short_description" class="col-md-3"><fmt:message key="jsp.tools.edit-community.form.label2"/></label>
                <span class="col-md-9"><input class="form-control" type="text" name="short_description" value="<%= Utils.addEntities(shortDesc) %>" size="50" />
                </span>
            </div><br/>
            <div class="row">
                <label class="col-md-3" for="introductory_text"><fmt:message key="jsp.tools.edit-community.form.label3"/></label>
                <span class="col-md-9"><textarea class="form-control" name="introductory_text" rows="6" cols="50"><%= Utils.addEntities(intro) %></textarea>
                </span>
            </div><br/>
            <div class="row">
                <label class="col-md-3" for="copyright_text"><fmt:message key="jsp.tools.edit-community.form.label4"/></label>
                <span class="col-md-9">
                    <textarea class="form-control" name="copyright_text" rows="6" cols="50"><%= Utils.addEntities(copy) %></textarea>
                </span>
            </div><br/>
        </div>
     </div>
 </div>
 <% if (community != null) { %>
 <div class="col-md-4">
 	<div class="panel panel-default">
		<div class="panel-heading"><fmt:message key="jsp.tools.edit-community.form.community-settings" /></div>
		<div class="panel-body">
<% if(bAdminCreateGroup || (admins != null && bAdminRemoveGroup)) { %>
 <%-- ===========================================================
     Community Administrators
     =========================================================== --%>
            <div class="row">
                <label class="col-md-6" for="submit_admins_create"><fmt:message key="jsp.tools.edit-community.form.label8"/></label>
                <span class="col-md-6 btn-group">

				 <div id="submit_admins_edit_div">
						<div class="globusGroupLink"><%= adminGroupsLink %></div>

                    <input class="btn btn-success col-md-12" type="submit"
                       name="submit_admins_edit"
                       id="submit_admins_edit"
                       <%  if(adminGroupsLink.equals("")) { %>
							value="<fmt:message key="jsp.tools.edit-community.form.button.select"/>"
						<%  } else { %>
							value="<fmt:message key="jsp.tools.edit-community.form.button.change"/>"
						<%  }  %>
                       />
					</div>
                </span>
            </div>

	<% } %>
	<%
    if (false && bPolicy) {

    %>

<%-- ===========================================================
     Edit community's policies
     =========================================================== --%>
            <div class="row">
                <label class="col-md-6" for="submit_authorization_edit"><fmt:message key="jsp.tools.edit-community.form.label7"/></label>
                <span class="col-md-6 btn-group">
                    <input class="col-md-12 btn btn-success" type="submit" name="submit_authorization_edit" value="<fmt:message key="jsp.tools.edit-community.form.button.edit"/>" />
                </span>
            </div>
    <% } %>

<%     if (false && bAdminCommunity) {
%>
<%-- ===========================================================
     Curate Community
     =========================================================== --%>
            <div class="row">
                <label for="submit_curate_community" class="col-md-6"><fmt:message key="jsp.tools.edit-community.form.label9"/></label>
                <span class="col-md-6">
                    <input class="col-md-12 btn btn-success" type="submit" name="submit_curate_community" value="<fmt:message key="jsp.tools.edit-community.form.button.curate"/>" />
                </span>
            </div>
    <% } %>
	</div>


	</div>
<%-- Globus Identifier Config --%>
<dspace:identifierconfig DSpaceObject="<%=community%>" context="<%=context %>"/>

</div>
<% } %>

</div>


<div class="row">
<div class="btn-group col-md-12">
<%
    if (community == null)
    {
%>
                        <input type="hidden" name="parent_community_id" value="<%= parentID %>" />
                        <input type="hidden" name="create" value="true" />
                        <input class="col-md-6 btn btn-success" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-community.form.button.create"/>" />

                        <input type="hidden" name="parent_community_id" value="<%= parentID %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COMMUNITY %>" />
                        <input class="col-md-6 btn btn-warning" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-community.form.button.cancel"/>" />
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="create" value="false" />
                        <input class="col-md-6 btn btn-success" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-community.form.button.update"/>" />

                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COMMUNITY %>" />
                        <input class="col-md-6 btn btn-warning" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-community.form.button.cancel"/>" />
<%
    }
%>
            </div>
        </div>
    </form>
</dspace:layout>
