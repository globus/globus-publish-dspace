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

<%--
  - Page that displays the netid/password login form
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.globus.GlobusAuth" %>

<dspace:layout navbar="off"
		locbar="off"
		titlekey="jsp.login.ldap.title">
		
<% String sessionId;
	if (request.getSession() != null) {
	    sessionId = ";jsession=" + request.getSession().getId();
	} else {
	    sessionId = "";
	}
%>
     <h4><fmt:message key="jsp.globusauth.description"/></h4>
     <p align="center" style="margin-top:75px;">
     
        <a href="<%= GlobusAuth.getAuthRedirectURL(request)%>" style="margin-left: 400px;" class="btn btn-primary col-md-3"><fmt:message key="jsp.globusauth.get.started"/></a>       
    </p>
</dspace:layout>
