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
  - UI page for selection of collection.
  -
  - Required attributes:
  -    collections - Array of collection objects to show in the drop-down.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.globus.GlobusUIUtil" %>
<%@ page import="org.dspace.globus.Globus" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<script src="/jspui/static/js/typeahead.bundle.js" type="text/javascript">

<%
    request.setAttribute("LanguageSwitch", "hide");

    //get collections to choose from
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");

	//check if we need to display the "no collection selected" error
    Boolean noCollection = (Boolean) request.getAttribute("no.collection");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
%>


<c:set var="dspace.layout.head" scope="request">
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.7.2.min.js"></script>
</c:set>
<c:set var="dspace.layout.head" scope="request">
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/typeahead.bundle.js"></script>
</c:set>
<c:set var="dspace.layout.head" scope="request">

		<script  type="text/javascript">
 $(document).ready(function() {
		 $('#downarrow').click(function() {
				 $('#typeahead-control').focus();
		 });

	function htmlDecode(value) {
        	return $("<div/>").html(value).text();
    	}

	var substringMatcher = function(strs) {
	  return function findMatches(q, cb) {
	    var matches, substringRegex;
	    matches = [];
	    substrRegex = new RegExp(q, 'i');
	    $.each(strs, function(i, str) {
	      if (substrRegex.test(str["value"])) {
	        matches.push(str);
	      }
	    });
	    cb(matches);
	  };
	};


	var collections = [
<%
        for (int i = 0; i < collections.length; i++){
%>
        {id : <%= collections[i].getID() %>, value:"<%= GlobusUIUtil.formatCollectionForDisplay(collections[i]) %>"}
<%
        	if (i < collections.length -1){
%>
		,
<%
		}
	}
%>
	];

	$('#scrollable-dropdown-menu .typeahead').typeahead({
	  hint: false,
	  highlight:true,
	  minLength: 0
	},{
	  name: 'collections',
	  limit: 45,
	  displayKey: "value",
	  source: substringMatcher(collections)
	}).on('typeahead:selected', function(event, data){
      $('#tcollection').val(data.id);
    });
 });

</script>
</c:set>

<c:set var="dspace.layout.head" scope="request">
<style>
#scrollable-dropdown-menu {
  width: 100%;
}

#scrollable-dropdown-menu .tt-menu {
  max-height: 200px;
  overflow-y: auto;
}
.typeahead,
.tt-query,
.tt-hint {
  width: 100%;
}

.typeahead {
  background-color: #fff;
}

.typeahead:focus {
  border: 2px solid #0097cf;
}

.tt-query {
  -webkit-box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
     -moz-box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
          box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
}

.tt-hint {
  color: #999
}

.twitter-typeahead{
  width: 100%;
}

.tt-menu {
  width: 100%;
  margin: 4px 0;
  padding: 8px 0;
  background-color: #fff;
  border: 1px solid #ccc;
  border: 1px solid rgba(0, 0, 0, 0.2);
  -webkit-border-radius: 8px;
     -moz-border-radius: 8px;
          border-radius: 8px;
  -webkit-box-shadow: 0 5px 10px rgba(0,0,0,.2);
     -moz-box-shadow: 0 5px 10px rgba(0,0,0,.2);
          box-shadow: 0 5px 10px rgba(0,0,0,.2);
}

.tt-suggestion {
  padding: 3px 20px;
  font-size: 16px;
  line-height: 22px;
}

.tt-suggestion:hover {
  cursor: pointer;
  color: #fff;
  background-color: #0097cf;
}

.tt-suggestion.tt-cursor {
  color: #fff;
  background-color: #0097cf;

}

.tt-suggestion p {
  margin: 0;
}

</style>
</c:set>

<dspace:layout style="submission" locbar="off"
               navbar="off"
               titlekey="jsp.submit.select-collection.title"
               nocache="true">

    <h1><fmt:message key="jsp.submit.select-collection.heading"/>
    <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#choosecollection\"%>"><fmt:message key="jsp.morehelp"/> </dspace:popup></h1>


<%  if (collections.length > 0)
    {
%>
	<p><fmt:message key="jsp.submit.select-collection.info1"/></p>

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">
<%
		//if no collection was selected, display an error
		if((noCollection != null) && (noCollection.booleanValue()==true))
		{
%>
					<div class="alert alert-warning"><fmt:message key="jsp.submit.select-collection.no-collection"/></div>
<%
		}
%>

					<div class="input-group">
<%--
					<label for="tcollection" class="input-group-addon">
						<fmt:message key="jsp.submit.select-collection.collection"/>
					</label>
--%>
					<div id="scrollable-dropdown-menu">
						<input class="form-control typeahead" type="text" placeholder="Select collection (start typing to filter the list)" id="typeahead-control">
					</div>

					<label id="downarrow" for="tcollection" class="input-group-addon glyphicon glyphicon-chevron-down">
					</label>

					<input type="hidden" name="collection" id="tcollection" value="-1">
                 <%--   <select class="form-control" name="collection" id="tcollection">
                    	<option value="-1"></option>
<%
        for (int i = 0; i < collections.length; i++)
        {
%>
                            <option value="<%= collections[i].getID() %>"><%= GlobusUIUtil.formatCollectionForDisplay(collections[i]) %></option>
<%
        }
%>
 --%>
                        </select>
					</div><br/>
            <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
            <%= SubmissionController.getSubmissionParameters(context, request) %>

				<div class="row">
					<div class="col-md-4 pull-right btn-group">
						<a href="<%=request.getContextPath() %><%=Globus.getPublishDashboardPath()%>"><span class="btn btn-default col-md-6"><fmt:message key="jsp.submit.select-collection.cancel"/> </span></a>
						<input class="btn btn-primary col-md-6" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
					</div>
				</div>
    </form>
<%  } else { %>
	<p class="alert alert-warning"><fmt:message key="jsp.submit.select-collection.none-authorized"/></p>
<%  } %>
	<!--
	   <p><fmt:message key="jsp.general.goto"/><br />
	   <a href="<%= request.getContextPath() %>"><fmt:message key="jsp.general.home"/></a><br />
	   <a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.general.mydspace" /></a>
	   </p>
	   	 -->
</dspace:layout>
