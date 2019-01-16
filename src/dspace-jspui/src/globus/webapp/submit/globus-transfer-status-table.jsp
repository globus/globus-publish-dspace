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


<%@ page language="java" contentType="text/html; charset=UTF-8"
	 pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page import="org.dspace.globus.GlobusUIUtil"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.text.DateFormat"%>
<%@ page import="org.globus.transfer.Task"%>
<%@ page import="org.globus.transfer.Task.Status"%>
<%@ page import="org.dspace.core.Context"%>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>
<%@ page import="org.dspace.globus.Globus"%>
<%@ page import="org.dspace.globus.GlobusWebAppIntegration"%>
<%@ page import="org.dspace.app.util.SubmissionInfo"%>
<%@ page import="org.dspace.content.Item"%>



<%
    String[] evenOdd = { "evenRow", "oddRow" };
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    Item item = subInfo.getSubmissionItem().getItem();
    String endpoint = item.getGlobusEndpoint();

    List<Task> taskList =
        (List<Task>) request.getAttribute(GlobusUIUtil.TASKLIST_REQUEST_ATTRIBUTE);
    if (taskList != null && taskList.size() > 0) {
        Locale locale = request.getLocale();
        DateFormat dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
%>

        <table class="table" align="center" summary="Table listing unfinished submissions">
            <tr>
                <th id="t10" class="EvenRowOddCol"><fmt:message key="jsp.globus-transfer-status-table.status"/></th>
                <th id="t11" class="EvenRowEvenCol"><fmt:message key="jsp.globus-transfer-status-table.source.endpoint"/></th>
                <th id="t12" class="EvenRowOddCol"><fmt:message key="jsp.globus-transfer-status-table.start.time"/></th>
                <th id="t13" class="EvenRowEvenCol"><fmt:message key="jsp.globus-transfer-status-table.completion.time"/></th>
                <th id="t12" class="EvenRowOddCol"><fmt:message key="jsp.globus-transfer-status-table.bytes.transferred"/></th>
            </tr>

	<%
	    for (int i = 0; i < taskList.size(); i++) {
	    	Task task = taskList.get(i);
	        String rowEvenOdd = evenOdd[(i + 1) % 2];
	        String manageUrl;
            manageUrl = GlobusWebAppIntegration.getWebAppActivityUrl(task.taskId);
	%>

			<tr>
				<td class="<%=rowEvenOdd%>OddCol">
					<a target="GlobusOps" href="<%=manageUrl%>"><%=task.status%></a>
				</td>
				<td class="<%=rowEvenOdd%>EvenCol"><%=task.sourceEndpoint%></td>
				<td class="<%=rowEvenOdd%>OddCol"><%=dateFormat.format(task.requestTime)%></td>
				<td class="<%=rowEvenOdd%>EvenCol">
					<%=(task.completionTime != null ? dateFormat.format(task.completionTime) : "---")%>
                </td>
				<td class="<%=rowEvenOdd%>OddCol"><%=task.bytesTransferred%></td>
			</tr>
	<%
	    }
	%>
		</table>
<%
    }
%>
