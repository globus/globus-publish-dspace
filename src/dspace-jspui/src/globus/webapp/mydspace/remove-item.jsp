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
  - Remove Item page
  -
  -  Attributes:
  -      workspace.item - the workspace item the user wishes to delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>

<%
    WorkspaceItem wi = (WorkspaceItem) request.getAttribute("workspace.item");
%>

<dspace:layout locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.remove-item.title"
               nocache="true">
<h2><fmt:message key="jsp.mydspace.remove-item.title"/></h2>

    <form action="<%= request.getContextPath() %>/mydspace" method="post">
    <span class="col-md-8"><fmt:message key="jsp.mydspace.remove-item.confirmation"/> </span>
        <input type="hidden" name="workspace_id" value="<%= wi.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.REMOVE_ITEM_PAGE %>"/>

 		<span class="col-md-4">
			<input class="btn btn-danger" type="submit" name="submit_delete" value="<fmt:message key="jsp.mydspace.remove-item.remove.button"/>" />
			<input class="btn btn-success" type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.remove-item.cancel.button"/>" />
 		</span>
    </form>

	<br/>
	<br/>

    <dspace:item item="<%= wi.getItem() %>"/>

</dspace:layout>
