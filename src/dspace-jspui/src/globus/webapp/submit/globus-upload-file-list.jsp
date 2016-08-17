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
  - List of uploaded files
  -
  - Attributes to pass in to this page:
  -   just.uploaded     - Boolean indicating if a file has just been uploaded
  -                       so a nice thank you can be displayed.
  -   show.checksums    - Boolean indicating whether to show checksums
  -
  - FIXME: Assumes each bitstream in a separate bundle.
  -        Shouldn't be a problem for early adopters.
  -
  - This file lists the uploaded files during the submission process
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="java.util.List" %>
<%@ page import="org.apache.commons.lang.time.DateFormatUtils" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.authorize.AuthorizeManager" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.globus.Globus" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    boolean justUploaded = ((Boolean) request.getAttribute("just.uploaded")).booleanValue();
    boolean showChecksums = ((Boolean) request.getAttribute("show.checksums")).booleanValue();

    request.setAttribute("LanguageSwitch", "hide");
    boolean allowFileEditing = !subInfo.isInWorkflow() || ConfigurationManager.getBooleanProperty("workflow", "reviewer.file-edit");

    boolean withEmbargo = ((Boolean)request.getAttribute("with_embargo")).booleanValue();

    List<ResourcePolicy> policies = null;
    String startDate = "";
    String globalReason = "";
    if (withEmbargo)
    {
        // Policies List
        policies = AuthorizeManager.findPoliciesByDSOAndType(context, subInfo.getSubmissionItem().getItem(), ResourcePolicy.TYPE_CUSTOM);

        startDate = "";
        globalReason = "";
        if (policies.size() > 0)
        {
            startDate = (policies.get(0).getStartDate() != null ? DateFormatUtils.format(policies.get(0).getStartDate(), "yyyy-MM-dd") : "");
            globalReason = policies.get(0).getRpDescription();
        }
    }

    boolean isAdvancedForm = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

%>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.submit.upload-file-list.title">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

<%--        <h1>Submit: <%= (justUploaded ? "File Uploaded Successfully" : "Uploaded Files") %></h1> --%>

<%
    if (justUploaded)
    {
%>
		<h1><fmt:message key="jsp.submit.upload-file-list.heading1"/>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#uploadedfile\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
		</h1>
        <p><fmt:message key="jsp.submit.upload-file-list.info1"/></p>
<%
    }
    else
    {
%>
	    <h1><fmt:message key="jsp.submit.upload-file-list.heading2"/>
	    <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#uploadedfile\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
	    </h1>
<%
    }
%>
<%-- Globus REMOVED all file listing --%>


<br/>


        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>


        <%  //if not first step, show "Previous" button
		if(!SubmissionController.isFirstStep(request, subInfo))
		{ %>
			<div class="col-md-6 pull-right btn-group">
				<input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
				<input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
				<input class="btn btn-primary col-md-4" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />

		<%  } else { %>
			<div class="col-md-4 pull-right btn-group">
				<input class="btn btn-default col-md-6" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
			    <input class="btn btn-primary col-md-6" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
		<%  }  %>
			</div>

    </form>

</dspace:layout>
