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
  - Location bar component
  -
  - This component displays the "breadcrumb" style navigation aid at the top
  - of most screens.
  -
  - Uses request attributes set in org.dspace.app.webui.jsptag.Layout, and
  - hence must only be used as part of the execution of that tag.  Plus,
  - dspace.layout.locbar should be verified to be true before this is included.
  -
  -  dspace.layout.parenttitles - List of titles of parent pages
  -  dspace.layout.parentlinks  - List of URLs of parent pages, empty string
  -                               for non-links
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.List" %>


<%-- Globus REMOVED - We don't want the breadcrumb bar --%>
<%--
<ol class="breadcrumb btn-success">
<%
    List parentTitles = (List) request.getAttribute("dspace.layout.parenttitles");
    List parentLinks = (List) request.getAttribute("dspace.layout.parentlinks");
 
    for (int i = 0; i < parentTitles.size(); i++)
    {
        String s = (String) parentTitles.get(i);
        String u = (String) parentLinks.get(i);

        if (u.equals(""))
        {
            if (i == parentTitles.size())
            {
%>
<li class="active"><%= s %></li>
<%
            }
            else
            {
%>
<li><%= s %></li>
<%			}
        }
        else
        {
%>
  <li><a href="<%= request.getContextPath() %><%= u %>"><%= s %></a></li>
<%
        }
}
Boolean admin = (Boolean)request.getAttribute("is.admin");
boolean isAdmin = (admin == null ? false : admin.booleanValue());
if (isAdmin)
{
%>
	<li><a href="<%= request.getContextPath() %>/dspace-admin">Administer</a></li>
<%
}
</ol>
 --%>
