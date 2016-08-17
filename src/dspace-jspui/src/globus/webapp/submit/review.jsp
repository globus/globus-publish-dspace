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
	Modifications Copyright 2014 University of Chicago
	All Rights Reserved.
 --%>

<%@ page contentType="text/html;charset=UTF-8"%>

<%@ page import="java.io.IOException"%>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ page import="javax.servlet.jsp.PageContext"%>

<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Iterator"%>

<%@ page import="org.dspace.app.webui.servlet.SubmissionController"%>
<%@ page import="org.dspace.submit.AbstractProcessingStep"%>
<%@ page import="org.dspace.app.util.DCInputSet"%>
<%@ page import="org.dspace.app.util.DCInput"%>
<%@ page import="org.dspace.app.util.SubmissionInfo"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>
<%@ page import="org.dspace.content.Bitstream"%>
<%@ page import="org.dspace.content.BitstreamFormat"%>
<%@ page import="org.dspace.content.DCDate"%>
<%@ page import="org.dspace.content.DCLanguage"%>
<%@ page import="org.dspace.content.DCValue"%>
<%@ page import="org.dspace.content.InProgressSubmission"%>
<%@ page import="org.dspace.content.Item"%>
<%@ page import="org.dspace.core.Context"%>
<%@ page import="org.dspace.core.Utils"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

    //get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    //get list of review JSPs from VerifyServlet
    HashMap reviewJSPs = (HashMap) request.getAttribute("submission.review");

    //get an iterator to loop through the review JSPs to load
    Iterator reviewIterator = reviewJSPs.keySet().iterator();
%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.submit.review.title"
	style="submission" nocache="true">

	<form action="<%=request.getContextPath()%>/submit" method="post"
		onkeydown="return disableEnterKey(event);">

		<jsp:include page="/submit/progressbar.jsp" />

		<h1>
			<fmt:message key="jsp.submit.review.heading" />
			<dspace:popup
				page="<%=LocaleSupport.getLocalizedMessage(pageContext,\"help.index\")
                        +\"#verify\"%>">
				<fmt:message key="jsp.morehelp" />
			</dspace:popup>
		</h1>

		<p>
			<fmt:message key="jsp.submit.review.info1" />
		</p>

		<div class="pane pane-default">
			<fmt:message key="jsp.submit.review.info2" />
		</div>

		<p>
			<fmt:message key="jsp.submit.review.info3" />
		</p>

		<p>
			<fmt:message key="jsp.submit.review.info4" />
		</p>
		<div class="container">
			<%
			    //loop through the list of review JSPs
			        while (reviewIterator.hasNext()) {
			            //remember, the keys of the reviewJSPs hashmap is in the
			            //format: stepNumber.pageNumber
			            String stepAndPage = (String) reviewIterator.next();

			            //finally get the path to the review JSP (the value)
			            String reviewJSP = (String) reviewJSPs.get(stepAndPage);
			%>
			<div class="well row">
				<%--Load the review JSP and pass it step & page info--%>
				<jsp:include page="<%=reviewJSP%>">
					<jsp:param name="submission.jump" value="<%=stepAndPage%>" />
				</jsp:include>
			</div>
			<%
			    }
			%>
		</div>
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
		<%=SubmissionController.getSubmissionParameters(context, request)%>

		<div class="col-md-7 pull-right btn-group">
			<input class="btn btn-default col-md-4" type="submit"
				name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>"
				value="<fmt:message key="jsp.submit.review.button.previous"/>" /> <input
				class="btn btn-default col-md-4" type="submit"
				name="<%=AbstractProcessingStep.CANCEL_BUTTON%>"
				value="<fmt:message key="jsp.submit.review.button.cancelsave"/>" /> <input
				class="btn btn-primary col-md-4" type="submit"
				name="<%=AbstractProcessingStep.NEXT_BUTTON%>" id="submitFinal"
				value="<fmt:message key="jsp.submit.review.button.submitfinal"/>" />
		</div>

	</form>

</dspace:layout>
