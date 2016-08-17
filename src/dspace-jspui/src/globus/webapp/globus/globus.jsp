<%--
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


<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ page import="org.dspace.content.Item" %>
<%
    String submissionID = (String) request.getAttribute("globus.submissionID");
    String handle = (String) request.getAttribute("globus.handle");
    String seq = (String) request.getAttribute("globus.sequenceID");
    Item item = (Item) request.getAttribute("item");

    //if (item!= null) {
    //	item.getHandle();
    //}
    String path = request.getContextPath() + "/handle/" + handle;
%>
<dspace:layout titlekey="jsp.tombstone.title">

    <h1>Transfer Started</h1>

    <p>Your transfer has been started with ID: <a href="https://www.globus.org/xfer/ViewActivity"><%=submissionID %> </a></p>
    <p align="center">
        <a href="<%= path %>/">Go to Dataset</a>
    </p>

    <p align="center">
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>

</dspace:layout>
