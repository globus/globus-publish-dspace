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
  - Preview task page
  -
  -   workflow.item:  The workflow item for the task they're performing
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>

<%
    WorkflowItem workflowItem =
        (WorkflowItem) request.getAttribute("workflow.item");

    Collection collection = workflowItem.getCollection();
    Item item = workflowItem.getItem();

    boolean showDatasetLink = true;
%>

<dspace:layout style="submission"
			   locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.preview-task.title"
               nocache="true">

	<h1><fmt:message key="jsp.mydspace.preview-task.title"/></h1>

<%
    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1POOL)
    {
        // In this state, the curator doesn't yet have permissions to view dataset
        showDatasetLink = false;
%>
	<p><fmt:message key="jsp.mydspace.preview-task.text1">
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
    </fmt:message></p>
<%
    }
    else if(workflowItem.getState() == WorkflowManager.WFSTATE_STEP2POOL)
    {
        showDatasetLink = false;
%>
	<p><fmt:message key="jsp.mydspace.preview-task.text3">
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
    </fmt:message></p>
<%
    }
    else if(workflowItem.getState() == WorkflowManager.WFSTATE_STEP3POOL)
    {
%>
	<p><fmt:message key="jsp.mydspace.preview-task.text4">
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
    </fmt:message></p>
<%
    }
%>

    <dspace:item item="<%= item %>" linkToDataset="<%= showDatasetLink %>"/>

    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.PREVIEW_TASK_PAGE %>"/>
		<input class="btn btn-default col-md-2" type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.general.cancel"/>" />
		<input class="btn btn-primary col-md-2 pull-right" type="submit" name="submit_start" value="<fmt:message key="jsp.mydspace.preview-task.accept.button"/>" />
    </form>
</dspace:layout>
