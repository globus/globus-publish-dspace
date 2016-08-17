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
  - Footer for home page
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
%>

            <%-- Right-hand side bar if appropriate --%>
<%
    if (sidebar != null)
    {
%>
	</div>
	<div class="col-md-3">
                    <%= sidebar %>
    </div>
    </div>
<%
    }
%>
</div>
</main>
            <%-- Page footer --%>
             <footer class="navbar-bottom">
             <div class="container glbs_footer">
			    <span class="globus_copyright">
			        &copy; 2010-<span class="year">2016</span> Computation Institute, University of Chicago, Argonne National Laboratory
			    </span>
			    <a href="https://www.globus.org/legal" class="globus_legal">legal</a>
			</div>
			<%--
             <div id="designedby" class="container text-muted">--%>
             <%--
             powered by  DSpace&nbsp;<img src="<%= request.getContextPath() %>/image/dspace-logo-only.png">

			<div id="footer_feedback" class="pull-right">
                                <p class="text-muted"><fmt:message key="jsp.layout.footer-default.text"/>&nbsp;-
                                <a target="_blank" href="<%= request.getContextPath() %>/feedback"><fmt:message key="jsp.layout.footer-default.feedback"/></a>
                                <a href="<%= request.getContextPath() %>/htmlmap"></a></p>
                                </div>
			--%><%--
			</div>--%>
    </footer>
    </body>
</html>
