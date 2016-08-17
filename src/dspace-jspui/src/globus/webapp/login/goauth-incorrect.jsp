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
  - Display message indicating login is incorrect
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<dspace:layout navbar="off"
			   locbar="nolink"
			   titlekey="jsp.login.ldap-incorrect.title">
    <table border="0" width="90%">
        <tr>
            <td align="left">
                <h1>Error logging in with your Globus identity</h1>
            </td>
            <td align="right" class="standard">
                <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#login\" %>"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
        </tr>
    </table>

    <p align="center">Either there was an error with your Globus identity or you are not permitted to
    access this instance. </p>
    <p align="center"> Please contact a system administrator or
    <a href="mailto:support@globus.org" target="_top">Globus Support</a></p>

</dspace:layout>
