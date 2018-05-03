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
  - Default navigation bar
--%>

<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.globus.Globus" %>
<%@ page import="org.dspace.globus.GlobusAuth" %>
<%@ page import="org.globus.GlobusClient" %>
<%@ page import="java.util.Map" %>
<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

	String globusBaseURL = Globus.getGlobusBaseURL();
    // Is the logged in user an admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring( 0, c );
    }

    String currentPageNoPath = currentPage;
    c = currentPage.lastIndexOf('/');
    if( c > -1 )
    {
    	currentPageNoPath = currentPageNoPath.substring( c + 1 );
    }

    // E-mail may have to be truncated
    String navbarEmail = null;

    if (user != null)
    {
        navbarEmail = user.getNetid();
        if (navbarEmail == null) {
            navbarEmail = user.getEmail();
        }
        if (navbarEmail == null) {
            navbarEmail = user.getName();
        }
    }

    // get the browse indices

	BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
    BrowseInfo binfo = (BrowseInfo) request.getAttribute("browse.info");
    String browseCurrent = "";
    if (binfo != null)
    {
        BrowseIndex bix = binfo.getBrowseIndex();
        // Only highlight the current browse, only if it is a metadata index,
        // or the selected sort option is the default for the index
        if (bix.isMetadataIndex() || bix.getSortOption() == binfo.getSortOption())
        {
            if (bix.getName() != null)
    			browseCurrent = bix.getName();
        }
    }
%>


<div class="navbar-header">
	<a title="Home" href="<%= globusBaseURL %>" class="logo navbar-btn pull-left"> <img
		alt="Home"
		src="<%= globusBaseURL %>/_site/image/logo_globus.png">
	</a> 
	<!-- .btn-navbar is used as the toggle for collapsed navbar content -->
	<button data-target=".navbar-collapse" data-toggle="collapse"
		class="navbar-toggle" type="button">
		<span class="sr-only"><fmt:message key="jsp.layout.navbar-default.toggle-navigation"/></span> <span class="icon-bar"></span>
		<span class="icon-bar"></span> <span class="icon-bar"></span>
	</button>
</div>

<div class="navbar-collapse collapse">
	<nav role="navigation">
		<div class="region region-navigation">
			<section class="block block-system block-menu clearfix"
				id="block-system-main-menu">
				<ul class="nav navbar-nav navbar-right">


				<%
				if (user != null){
				%>
					<li class="expanded dropdown">
						<a data-toggle="dropdown" data-target="#" class="go-nav-item go-logged-in dropdown-toggle" href="/home/%23">Manage Data <span class="caret"></span></a>
						<ul class="dropdown-menu">
							<li class="first leaf"><a href="<%= globusBaseURL %>/xfer/StartTransfer">Transfer Files</a></li>
							<li class="leaf"><a href="<%= globusBaseURL %>/xfer/ViewActivity">Activity</a></li>
							<li class="leaf"><a href="<%= globusBaseURL %>/xfer/ManageEndpoints">Manage Endpoints</a></li>
							<li class="last leaf"><a href="<%= globusBaseURL %>/Dashboard">Dashboard</a></li>
						</ul>
					</li>
					<li class="first leaf"><a class="go-nav-item go-logged-out dropdown-menu-selected"
						href="<%= request.getContextPath() %><%= Globus.getPublishDashboardPath() %>">Publish</a>
					</li>
					<li class="leaf"><a class="go-nav-item go-logged-out"
						href="<%= globusBaseURL %>/Groups">Groups</a></li>
					<li class="expanded dropdown">
						<a data-toggle="dropdown" class="dropdown-toggle" data-target="#" href="/home/%23">Support <span class="caret"></span></a>
						<ul class="dropdown-menu">
							<li class="first leaf"><a class="zendeskLink zendeskChecked" href="https://support.globus.org/forums">Forums</a></li>
							<li class="leaf"><a class="zendeskLink zendeskChecked" href="https://support.globus.org/forums/20538617">Quick Start Guides</a></li>
							<li class="leaf"><a class="zendeskLink zendeskChecked" href="https://support.globus.org/forums/20534338">FAQs</a></li>
							<li class="leaf"><a class="zendeskLink zendeskChecked" href="https://support.globus.org/entries/24044351">Downloads</a></li>
							<li class="leaf"><a class="zendeskLink zendeskChecked" href="https://support.globus.org/categories/20133407">Provider Documentation</a></li>
							<li class="leaf"><a class="zendeskLink zendeskChecked" href="https://www.globus.org/contact-us">Contact Us</a></li>
							<li class="last leaf"><a class="zendeskLink zendeskChecked" href="https://support.globus.org/requests">Check Support Tickets</a></li>
						</ul>
					</li>
					<li class="expanded dropdown">
						<a data-toggle="dropdown" class="dropdown-toggle" data-target="#" href="/home/%23">Account<span class="caret"></span></a>
						<ul class="dropdown-menu">
							<li class="first leaf"><a href="<%= globusBaseURL %>/app/account">Manage Account</a></li>
							<li class="leaf"><span class="divider separator"></span></li>
							<%
							if (isAdmin)
							{
							%>
							<li class="leaf"><a class="go-logout" href="<%= request.getContextPath() %>/dspace-admin">Admin</a></li>
							<%
							}
							%>
							<li class="last leaf"><a class="go-logout" href="<%= request.getContextPath() %>/logout">Logout</a></li>						
						</ul>
					</li>
				<%
				} else {

				%>
					<li class="leaf"><a class="go-nav-item go-logged-out"
							href="<%= GlobusAuth.getAuthRedirectURL(request)%>">Log In</a></li>
					<li class="last leaf"><a class="go-nav-item go-logged-out"
							href="https://www.globus.org/SignUp">Sign Up</a></li>
				<%
				}
				%>
				</ul>

			</section>
			<!-- /.block -->
		</div>
	</nav>
	<%
	if (user !=null)
	{
	%>
	<div class="row2">

      <div class="row2Content glbs_heading_subnav subnav glbs_header_sub_nav"><ul class="u2">
     <%
      if (currentPageNoPath.equals("") || currentPageNoPath.startsWith("home") || currentPageNoPath.startsWith("simple-search")){
      %>
      	<li class="l2 glbs_first glbs_selected">
      <% } else { %>
        <li class="l2 glbs_first">
      <% } %>

      <a href="/jspui" class="a2"><fmt:message key="jsp.layout.navbar-default.discover"/></a></li>

      <%
      if (currentPageNoPath.startsWith("PublishDashboard")){
      %>
      	<li class="l2 glbs_selected">
      <% } else { %>
        <li class="l2">
      <% } %>

      <a href="/jspui/PublishDashboard" class="a2"><fmt:message key="jsp.layout.navbar-default.publishdashboard"/></a></li>

      <%
      if (currentPageNoPath.startsWith("community-list")){
      %>
      	<li class="l2 glbs_selected">
      <% } else { %>
        <li class="l2">
      <% } %>

      <a href="/jspui/community-list" class="a2"><fmt:message key="jsp.layout.navbar-default.communitylist"/></a></li>
    
      
      </ul></div>
    </div>
    <%
    } else {
    %>
    	<div class="row2">
	      <div class="row2Content glbs_heading_subnav subnav glbs_header_sub_nav" style="line-height: 22px;">
	      	<ul class="u2 nav-justified alert alert-info" style="padding-top: 2px; padding-bottom: 2px;">
	      		<li style="text-align:center;"><fmt:message key="jsp.general.pleaselogin"/></li>
	      	</ul>
	      </div>
    	</div>
    <%
    }
    %>
</div>
