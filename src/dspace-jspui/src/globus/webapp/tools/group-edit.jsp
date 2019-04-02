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
  - Show contents of a group (name, epeople)
  -
  - Attributes:
  -   group - group to be edited
  -
  - Returns:
  -   cancel - if user wants to cancel
  -   add_eperson - go to group_eperson_select.jsp to choose eperson
  -   change_name - alter name & redisplay
  -   eperson_remove - remove eperson & redisplay
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group"   %>
<%@ page import="org.dspace.globus.Globus"   %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.handle.HandleManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.DSpaceObject" %>
<%
    Group group = (Group) request.getAttribute("group");
    EPerson [] epeople = (EPerson []) request.getAttribute("members");

	Group   [] groups  = (Group   []) request.getAttribute("membergroups");
	request.setAttribute("LanguageSwitch", "hide");

	String collectionHandle = (String) request.getAttribute("collection_handle");
	String communityHandle = (String) request.getAttribute("community_handle");

	Context context = UIUtil.obtainContext(request);
	int collectionId = -1;
	Collection collection = null;
	if (collectionHandle != null) {
	    collection = (Collection) HandleManager.resolveToObject(context, collectionHandle);
	    if (collection != null) {
	        collectionId = collection.getID();
	    }
	}

	int communityId = -1;
	Community community = null;
	if (communityHandle != null) {
	    community = (Community) HandleManager.resolveToObject(context, communityHandle);
	    if (community != null) {
	        communityId = community.getID();
	    }
	}
%>

<dspace:layout style="submission" titlekey="jsp.tools.group-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <h1><fmt:message key="jsp.tools.group-edit.title"/> : <%=Utils.addEntities(group.getName())%> (id: <%=group.getID()%>)
  </h1>
          <form name="epersongroup" method="post" action="">
            
	<br/>
    <%--  <div class="alert alert-warning"><fmt:message key="jsp.tools.group-edit.heading"/></div> --%>

    <input type="hidden" name="group_id" value="<%=group.getID()%>"/>

    <% if (collectionHandle != null){
     %>

    <input type="hidden" name="collection_handle" value=<%= collectionHandle %>/>
    <% } if (communityHandle != null) { %>
	<input type="hidden" name="community_handle" value=<%= communityHandle %>/>
	<% } %>

    <%-- Globus altered to pass in group ID --%>
    <div class="row">
        <div class="col-md-12">
            <label for="eperson_id"><fmt:message key="jsp.tools.group-edit.group"/></label>
	    	<dspace:selectgroup  multiple="true" selected="<%= groups  %>" groupId="<%= group.getID() %>" collectionHandle="<%= collectionHandle %>" communityHandle="<%= communityHandle %>"/>
        </div>
      </div>
	<br/>

    <div class="row"><input class="btn btn-success col-md-2 col-md-offset-5" type="submit" name="submit_group_update" value="<fmt:message key="jsp.tools.group-edit.update.button"/>" onclick="javascript:finishGroups();"/></div>
   </form>
   <br/>
   <br/>
 <% if (collectionHandle != null) {

 		if (collectionId != -1) {
 %>
		    <!--  Need to submit as form to jump back to edit page  -->
 		    <form id="edit_coll" method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
 		    	<input type="hidden" name="collection_id" value="<%= collectionId %>" />

 <%			if (communityId != -1) { %>
 		        <input type="hidden" name="community_id" value="<%= communityId %>" />
<% 			} %>
 		        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION %>" />
			   <div class="row col-md-offset-5">
			     <a href="javascript:{}" onclick="document.getElementById('edit_coll').submit(); return false;"><fmt:message key="jsp.tools.group-edit.collection.return"/></a>
			   </div>
 		    </form>
  <%
 		} else {
 		    /* If we don't have the object, we jump back to the landing page based on the handle */
%>
		   <div class="row col-md-offset-5"><a href="<%= request.getContextPath() %>/handle/<%= collectionHandle %>"><fmt:message key="jsp.tools.group-edit.collection.return"/></a></div>
<%
 		}
 %>

 <% } if (communityHandle != null) {
     	if (communityId != -1) {
%>
		    <!--  Need to submit as form to jump back to edit page  -->
 		    <form id="edit_comm" method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
 		        <input type="hidden" name="community_id" value="<%= communityId %>" />
 		        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COMMUNITY %>" />
			    <div class="row col-md-offset-5">
			     <a href="javascript:{}" onclick="document.getElementById('edit_comm').submit(); return false;"><fmt:message key="jsp.tools.group-edit.community.return"/></a>
			   </div>
			   </form>

<%   	} else { %>
   <div class="row col-md-offset-5"><a href="<%= request.getContextPath() %>/handle/<%= communityHandle %>"><fmt:message key="jsp.tools.group-edit.community.return"/></a></div>
 <%
 		}
     }
 %>

</dspace:layout>
