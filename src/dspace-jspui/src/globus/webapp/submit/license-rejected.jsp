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
  - License rejected page
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<dspace:layout style="submission" navbar="off" locbar="off" titlekey="jsp.submit.license-rejected.title">

    <%-- <h1>Submit: License Rejected</h1> --%>
	<h1><fmt:message key="jsp.submit.license-rejected.heading"/></h1>

    <%-- <p>You have chosen not to grant the license to distribute your submission
    via the DSpace system.  Your submission has not been deleted and can be
    accessed from the My DSpace page.</p> --%>
	<p><fmt:message key="jsp.submit.license-rejected.info1"/></p>

    <%-- <p>If you wish to contact us to discuss the license, please use one
    of the methods below:</p> --%>
	<p><fmt:message key="jsp.submit.license-rejected.info2"/></p>

	<!--
    <dspace:include page="/components/contact-info.jsp" />
 	-->

    <%-- <p><a href="<%= request.getContextPath() %>/mydspace">Go to My DSpace</a></p> --%>
	<p><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.goto-mydspace"/></a></p>

</dspace:layout>
