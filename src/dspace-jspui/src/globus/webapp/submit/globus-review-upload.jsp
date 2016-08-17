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
  - Review file upload info
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>
<%@ page contentType="text/html;charset=UTF-8"%>

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.dspace.authorize.AuthorizeManager"%>
<%@page import="org.dspace.authorize.ResourcePolicy"%>
<%@page import="java.util.List"%>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController"%>
<%@ page import="org.dspace.app.util.SubmissionInfo"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>
<%@ page import="org.dspace.content.Bitstream"%>
<%@ page import="org.dspace.content.BitstreamFormat"%>
<%@ page import="org.dspace.content.Item"%>
<%@ page import="org.dspace.core.Context"%>
<%@ page import="org.dspace.core.Utils"%>
<%@ page import="org.dspace.globus.Globus"%>


<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ page import="javax.servlet.jsp.PageContext"%>
<%@ page import="org.dspace.globus.GlobusUIUtil" %>
<%@ page import="org.dspace.globus.Globus" %>
<%@ page import="org.globus.GlobusClient" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

    //get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    boolean advanced =
        ConfigurationManager
            .getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

    //get the step number (for jump-to link)
    String stepJump = (String) request.getParameter("submission.jump");

    Item item = subInfo.getSubmissionItem().getItem();

    //is advanced upload embargo step?
    Object isUploadWithEmbargoB = request.getAttribute("submission.step.uploadwithembargo");
    boolean isUploadWithEmbargo = false;
    if (isUploadWithEmbargoB != null) {
        isUploadWithEmbargo = (Boolean) isUploadWithEmbargoB;
    }

    String epName = item.getGlobusEndpoint();
    String epPath = item.getGlobusSharePath();

    Globus g = Globus.getGlobusClientFromContext(context);
    if (g != null){
    	GlobusClient client = g.getClient();

    	GlobusUIUtil.getTasksForDestEndpoint(request, client, epName, epPath, true);
    	GlobusUIUtil.getTasksForDestEndpoint(request, Globus.getPrivlegedClient(), epName, epPath, false);

    }
%>

<script>
function modifyClick() {
	var globusWin = window.open("<%=Globus.getTransferPage(epName, epPath)%>", "GlobusOps");
	globusWin.focus();
}
</script>

<%-- ====================================================== --%>
<%--                    UPLOADED_FILES                      --%>
<%-- ====================================================== --%>
<div class="row col-md-12">
<%--
		<span class="metadataFieldLabel col-md-2"><%=(subInfo.getSubmissionItem().hasMultipleFiles() ? LocaleSupport
                .getLocalizedMessage(pageContext, "jsp.submit.review.upload1") : LocaleSupport
                .getLocalizedMessage(pageContext, "jsp.submit.review.upload2"))%></span>
        <br/>
	<div class="row">
--%>
        <span class="pull-right col-md-2"><button type="button" class="btn btn-default" onClick="modifyClick()">&nbsp;Review Files</button></span>

	<span class="metadataFieldValue col-md-10">

		<dspace:transferlist activeheader="Transfers initiated to assemble dataset:" inactiveheader="No transfers in progress"/>

	</span>

<%--
	</div>
 --%>
</div>

<%-- ====================================================== --%>
<%--                    ARTIFACT FILE                      --%>
<%-- ====================================================== --%>
<div class="col-md-12">
	<div class="row">
		<div class="metadataFieldLabel col-md-6"><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.manifest")%>
		 		 <dspace:transferlist metadata="true" summaryOnly="true"/>
		 </div>
		<span class="metadataFieldValue col-md-12">
		<!--
		<jsp:include page="/submit/globus-transfer-artifact-table.jsp"/>
		 -->


		</span>
	</div>
</div>
