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
  - Main My DSpace page
  -
  -
  - Attributes:
  -    mydspace.user:    current user (EPerson)
  -    workspace.items:  WorkspaceItem[] array for this user
  -    workflow.items:   WorkflowItem[] array of submissions from this user in
  -                      workflow system
  -    workflow.owned:   WorkflowItem[] array of tasks owned
  -    workflow.pooled   WorkflowItem[] array of pooled tasks
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.SupervisedItem" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group"   %>
<%@ page import="org.dspace.globus.Globus" %>
<%@ page import="org.dspace.globus.GlobusUIUtil" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.util.Locale" %>

<%
    EPerson user = (EPerson) request.getAttribute("mydspace.user");

    WorkspaceItem[] workspaceItems =
        (WorkspaceItem[]) request.getAttribute("workspace.items");

    WorkflowItem[] workflowItems =
        (WorkflowItem[]) request.getAttribute("workflow.items");

    WorkflowItem[] owned =
        (WorkflowItem[]) request.getAttribute("workflow.owned");

    WorkflowItem[] pooled =
        (WorkflowItem[]) request.getAttribute("workflow.pooled");

    Group [] groupMemberships =
        (Group []) request.getAttribute("group.memberships");

    SupervisedItem[] supervisedItems =
        (SupervisedItem[]) request.getAttribute("supervised.items");

    List<String> exportsAvailable = (List<String>)request.getAttribute("export.archives");

    // Is the logged in user an admin
    Boolean displayMembership = (Boolean)request.getAttribute("display.groupmemberships");
    boolean displayGroupMembership = (displayMembership == null ? false : displayMembership.booleanValue());

    String dashboardPath = request.getContextPath() + Globus.getPublishDashboardPath();
	Locale locale = request.getLocale();
	DateFormat dateFormat = null;
	if (locale != null) {
	    dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
	}
%>

<dspace:layout style="submission" titlekey="jsp.mydspace" nocache="true">
	<div class="panel">
        <form class="form" role="search" action="/jspui/simple-search" method="GET">
        <div class="input-group">
            <input type="text" class="form-control search_bar" placeholder="<fmt:message key="jsp.search.title" />" name="query" id="srch-term"  style="font-size: 17px;">
            <div class="input-group-btn">
                <button class="btn btn-default search-button" type="submit"><i class="glyphicon glyphicon-search"></i></button>
            </div>
        </div>
        </form>
    </div>

	<div class="panel panel-default">
        <div class="panel-heading">
        			<%-- Globus Removed username from title --%>
                    <fmt:message key="jsp.mydspace"/>
        </div>

		<div class="panel-body">
		    <form action="<%= dashboardPath %>" method="post">
		        <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                <input class="btn btn-success" type="submit" name="submit_new" value="<fmt:message key="jsp.mydspace.main.start.button"/>" />
                <input class="btn btn-default" type="submit" name="submit_own" value="<fmt:message key="jsp.mydspace.main.view.button"/>" />
		    </form>


<%-- Task list:  Only display if the user has any tasks --%>
<%
    if (owned.length > 0)
    {
%>
    <h3><fmt:message key="jsp.mydspace.main.heading2"/></h3>

    <p class="submitFormHelp">
        <%-- Below are the current tasks that you have chosen to do. --%>
        <fmt:message key="jsp.mydspace.main.text1"/>
    </p>

    <table class="table" align="center" summary="Table listing owned tasks">
        <tr>
            <th id="own1" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.item"/></th>
            <th id="own2" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.subto"/></th>
            <th id="own3" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.subby"/></th>
            <th id="own4" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.task"/></th>
            <th id="own5" class="oddRowOddCol">&nbsp;</th>
        </tr>
<%
        // even or odd row:  Starts even since header row is odd (1).  Toggled
        // between "odd" and "even" so alternate rows are light and dark, for
        // easier reading.
        String row = "even";

        for (int i = 0; i < owned.length; i++)
        {
            WorkflowItem wfItem = owned[i];
            Item item = wfItem.getItem();
            DCValue[] titleArray = item.getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                                                  : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = item.getSubmitter();
            Collection coll = wfItem.getCollection();
%>
        <tr>
                <td headers="own1" class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
                <td headers="own2" class="<%= row %>RowEvenCol"><%= GlobusUIUtil.formatCollectionForDisplay(coll, 1) %></td>
                <td headers="own3" class="<%= row %>RowOddCol"><a href="mailto:<%= submitter.getEmail() %>"><%= Utils.addEntities(submitter.getFullName()) %></a></td>
                <td headers="own4" class="<%= row %>RowEvenCol">
<%
            switch (wfItem.getState())
            {

            //There was once some code...
            case WorkflowManager.WFSTATE_STEP1: %><fmt:message key="jsp.mydspace.main.sub1"/><% break;
            case WorkflowManager.WFSTATE_STEP2: %><fmt:message key="jsp.mydspace.main.sub2"/><% break;
            case WorkflowManager.WFSTATE_STEP3: %><fmt:message key="jsp.mydspace.main.sub3"/><% break;
            }
%>
                </td>
                <!-- <td headers="t5" class="<%= row %>RowOddCol"></td> -->
                <td headers="own5" class="<%= row %>RowOddCol">
                     <form action="<%= dashboardPath %>" method="post">
                        <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                        <input type="hidden" name="workflow_id" value="<%= wfItem.getID() %>" />
                        <input class="btn btn-primary" type="submit" name="submit_perform" value="<fmt:message key="jsp.tools.curate.perform.button"/>" />
                        <input class="btn btn-default" type="submit" name="submit_return" value="<fmt:message key="jsp.mydspace.main.return.button"/>" />
                     </form>
                </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
    </table>
<%
    }

    // Pooled tasks - only show if there are any
    if (pooled.length > 0)
    {
%>
    <h3><fmt:message key="jsp.mydspace.main.heading3"/></h3>

    <p class="submitFormHelp">
        <%--Below are tasks in the task pool that have been assigned to you. --%>
        <fmt:message key="jsp.mydspace.main.text2"/>
    </p>

    <table class="table" align="center" summary="Table listing the tasks in the pool">
        <tr>
            <th id="p1" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.item"/></th>
            <th id="p2" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.subto"/></th>
            <th id="p3" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.subby"/></th>
            <th id="p4" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.task"/></th>
            <th id="p5" class="oddRowOddCol"> </th>
        </tr>
<%
        // even or odd row:  Starts even since header row is odd (1).  Toggled
        // between "odd" and "even" so alternate rows are light and dark, for
        // easier reading.
        String row = "even";

        for (int i = 0; i < pooled.length; i++)
        {
            WorkflowItem wfItem = pooled[i];
            Item item = wfItem.getItem();
            Collection coll = wfItem.getCollection();
            DCValue[] titleArray = item.getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = item.getSubmitter();
%>
        <tr>
                    <td headers="p1" class="<%= row %>RowEvenCol"><%= Utils.addEntities(title) %></td>
                    <td headers="p2" class="<%= row %>RowOddCol"><%= GlobusUIUtil.formatCollectionForDisplay(coll, 1) %></td>
                    <td headers="p3" class="<%= row %>RowEvenCol"><a href="mailto:<%= submitter.getEmail() %>"><%= Utils.addEntities(submitter.getFullName()) %></a></td>
                    <td headers="p4" class="<%= row %>RowOddCol">
<%
            switch (pooled[i].getState())
            {
            case WorkflowManager.WFSTATE_STEP1POOL: %><fmt:message key="jsp.mydspace.main.sub1"/><% break;
            case WorkflowManager.WFSTATE_STEP2POOL: %><fmt:message key="jsp.mydspace.main.sub2"/><% break;
            case WorkflowManager.WFSTATE_STEP3POOL: %><fmt:message key="jsp.mydspace.main.sub3"/><% break;
            }
%>
                    </td>
                    <td headers="p5" class="<%= row %>RowOddCol">
                        <form action="<%= dashboardPath %>" method="post">
                            <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                            <input type="hidden" name="workflow_id" value="<%= pooled[i].getID() %>" />
                            <input class="btn btn-default" type="submit" name="submit_claim" value="<fmt:message key="jsp.mydspace.main.take.button"/>" />
                        </form>
                    </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even");
        }
%>
    </table>
<%
    }

    // Display workspace items (authoring or supervised), if any
    if (workspaceItems.length > 0 || supervisedItems.length > 0)
    {
        // even or odd row:  Starts even since header row is odd (1)
        String row = "even";
%>

    <h3><fmt:message key="jsp.mydspace.main.heading4"/></h3>

    <p><fmt:message key="jsp.mydspace.main.text4" /></p>

    <table class="table" align="center" summary="Table listing unfinished submissions">
        <tr>
            <th id="t10" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.elem1"/></th>
            <th id="t11" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.elem2"/></th>
            <th id="t12" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.timestamp"/></th>
            <th id="t13" class="oddRowOddCol">&nbsp;</th>
            <th id="t14" class="oddRowEvenCol">&nbsp;</th>
            <th id="t15" class="oddRowOddCol">&nbsp;</th>
        </tr>
<%
        if (supervisedItems.length > 0 && workspaceItems.length > 0)
        {
%>
        <tr>
            <th colspan="5">
                <%-- Authoring --%>
                <fmt:message key="jsp.mydspace.main.authoring" />
            </th>
        </tr>
<%
        }

        for (int i = 0; i < workspaceItems.length; i++)
        {
            WorkspaceItem wsItem = workspaceItems[i];
            Item item = wsItem.getItem();
            Collection coll = wsItem.getCollection();
            DCValue[] titleArray = item.getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = item.getSubmitter();
            Date d = item.getLastModified();
            String lastMod = null;
            if (dateFormat != null) {
                lastMod = dateFormat.format(d);
            } else {
                lastMod = d.toString();
            }
%>
        <tr>
            <td headers="t10" class="<%= row %>RowEvenCol"><%= Utils.addEntities(title) %></td>
            <td headers="t11" class="<%= row %>RowOddCol"><%= GlobusUIUtil.formatCollectionForDisplay(coll, 1) %></td>
            <td headers="t12" class="<%= row %>RowEvenCol"><%= lastMod %></td>

	        <td headers="t13" class="<%= row %>RowOddCol" style="width:25px">
                <form action="<%= dashboardPath %>" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                    <input type="hidden" name="workspace_id" value="<%= wsItem.getID() %>"/>
                    <input type="hidden" name="resume" value="<%= wsItem.getID() %>"/>
                    <input class="btn btn-default" type="submit" name="submit_resume" value="<fmt:message key="jsp.tools.general.resume"/>"/>
                </form>
            </td>
	        <td headers="t14" class="<%= row %>RowEvenCol" style="width:25px">
                <form action="<%= request.getContextPath() %>/view-workspaceitem" method="post">
                   <input type="hidden" name="workspace_id" value="<%= wsItem.getID() %>"/>
                   <input class="btn btn-default" type="submit" name="submit_view" value="<fmt:message key="jsp.tools.general.view"/>"/>
                </form>
            </td>
            <td headers="t15" class="<%= row %>RowOddCol" style="width:25px">
                <form action="<%= dashboardPath %>" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                    <input type="hidden" name="workspace_id" value="<%= wsItem.getID() %>"/>
                    <input class="btn btn-default" type="submit" name="submit_delete" value="<fmt:message key="jsp.mydspace.general.remove"/>"/>
                </form>
            </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>

<%-- Start of the Supervisors workspace list --%>
<%
        if (supervisedItems.length > 0)
        {
%>
        <tr>
            <th colspan="5">
                <fmt:message key="jsp.mydspace.main.supervising" />
            </th>
        </tr>
<%
        }

        for (int i = 0; i < supervisedItems.length; i++)
        {
            SupervisedItem si = supervisedItems[i];
            Item item = si.getItem();
            DCValue[] titleArray = item.getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            EPerson submitter = item.getSubmitter();
            Collection coll = si.getCollection();
%>

        <tr>
            <td class="<%= row %>RowOddCol">
                <form action="<%= request.getContextPath() %>/workspace" method="post">
                    <input type="hidden" name="workspace_id" value="<%= si.getID() %>"/>
                    <input class="btn btn-default" type="submit" name="submit_open" value="<fmt:message key="jsp.mydspace.general.open" />"/>
                </form>
            </td>
            <td class="<%= row %>RowEvenCol">
                <a href="mailto:<%= submitter.getEmail() %>"><%= Utils.addEntities(submitter.getFullName()) %></a>
            </td>
            <td class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
            <td class="<%= row %>RowEvenCol"><%= GlobusUIUtil.formatCollectionForDisplay(coll, 1) %></td>
            <td class="<%= row %>RowOddCol">
                <form action="<%= dashboardPath %>" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                    <input type="hidden" name="workspace_id" value="<%= supervisedItems[i].getID() %>"/>
                    <input class="btn btn-default" type="submit" name="submit_delete" value="<fmt:message key="jsp.mydspace.general.remove" />"/>
                </form>
            </td>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
    </table>
<%
    }
%>

<%
    // Display workflow items, if any
    if (workflowItems.length > 0)
    {
        // even or odd row:  Starts even since header row is odd (1)
        String row = "even";
%>
    <h3><fmt:message key="jsp.mydspace.main.heading5"/></h3>

    <table class="table" align="center" summary="Table listing submissions in workflow process">
        <tr>
            <th id="wf1" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.elem1"/></th>
            <th id="wf2" class="oddRowEvenCol"><fmt:message key="jsp.mydspace.main.elem2"/></th>
            <th id="wf3" class="oddRowOddCol"><fmt:message key="jsp.mydspace.main.timestamp"/></th>
            <th id="wf4" class="oddRowEvenCol"> </th>
            <%--
            <th id="wf5" class="oddRowEvenCol"> </th>
             --%>
        </tr>
<%
        for (int i = 0; i < workflowItems.length; i++)
        {
            WorkflowItem wfItem = workflowItems[i];
            Item item = wfItem.getItem();
            DCValue[] titleArray = item.getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                    : LocaleSupport.getLocalizedMessage(pageContext,"jsp.general.untitled") );
            Collection coll = wfItem.getCollection();
            Date d = item.getLastModified();
            String lastMod = null;
            if (dateFormat != null) {
                lastMod = dateFormat.format(d);
            } else {
                lastMod = d.toString();
            }
%>
            <tr>
                <td headers="wf1" class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
                <td headers="wf2" class="<%= row %>RowEvenCol">
                   <form action="<%= dashboardPath %>" method="post">
                       <%= GlobusUIUtil.formatCollectionForDisplay(coll, 1) %>
                       <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                       <input type="hidden" name="workflow_id" value="<%= wfItem.getID() %>" />
                   </form>
                </td>
                <td headers="wf3" class="<%= row %>RowOddCol"><%=lastMod%></td>
	        <td headers="wf4" class="<%= row %>RowEvenCol" style="width:25px">
                <form action="<%= dashboardPath %>" method="post">
                   <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>" />
                   <input type="hidden" name="workflow_id" value="<%= wfItem.getID() %>"/>
                   <input class="btn btn-default" type="submit" name="submit_view" value="<fmt:message key="jsp.tools.general.view"/>"/>
                </form>
            </td>
            <%--
            <td headers="wf5" class="<%= row %>RowOddCol" style="width:25px">
                <form action="<%= dashboardPath %>" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REJECT_REASON_PAGE %>" />
                    <input type="hidden" name="workflow_id" value="<%= wfItem.getID() %>"/>
                    <input class="btn btn-default" type="submit" name="submit_send" value="Remove From Curation"/>
                </form>
            </td>
             --%>
            </tr>
<%
      row = (row.equals("even") ? "odd" : "even" );
    }
%>
    </table>
<%
  }

  if(displayGroupMembership && groupMemberships.length>0)
  {
%>
    <h3><fmt:message key="jsp.mydspace.main.heading6"/></h3>
    <ul>
<%
    for(int i=0; i<groupMemberships.length; i++)
    {
%>
    <li><%=groupMemberships[i].getName()%></li>
<%
    }
%>
	</ul>
<%
  }
%>

	<%if(exportsAvailable!=null && exportsAvailable.size()>0){ %>
	<h3><fmt:message key="jsp.mydspace.main.heading7"/></h3>
	<ol class="exportArchives">
		<%for(String fileName:exportsAvailable){%>
			<li><a href="<%=request.getContextPath()+"/exportdownload/"+fileName%>" title="<fmt:message key="jsp.mydspace.main.export.archive.title"><fmt:param><%= fileName %></fmt:param></fmt:message>"><%=fileName%></a></li>
		<% } %>
	</ol>
	<%} %>
	</div>
</div>
</dspace:layout>
