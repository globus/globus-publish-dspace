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
- Show form allowing edit of collection metadata
-
- Attributes:
-    community    - community to create new collection in, if creating one
-    collection   - collection to edit, if editing an existing one.  If this
-                  is null, we are creating one.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Collection.PolicyType" %>
<%@ page import="org.dspace.content.DSpaceObject" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.harvest.HarvestedCollection" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="org.dspace.globus.Globus" %>
<%@ page import="org.dspace.globus.GlobusWebAppIntegration" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<fmt:message key="jsp.tools.edit-collection.form.button.select" var="selectSelectButtonTitle"/>
<fmt:message key="jsp.tools.edit-collection.form.button.change" var="selectChangeButtonTitle"/>
<%
Context context = UIUtil.obtainContext(request);
Collection collection = (Collection) request.getAttribute("collection");
Community community = (Community) request.getAttribute("community");
DSpaceObject objToIntrospect = (collection != null ? collection : community);

Boolean adminCollection = (Boolean)request.getAttribute("admin_collection");
boolean bAdminCollection = (adminCollection == null ? false : adminCollection.booleanValue());

Boolean adminCreateGroup = (Boolean)request.getAttribute("admin_create_button");
boolean bAdminCreateGroup = (adminCreateGroup == null ? false : adminCreateGroup.booleanValue());

Boolean adminRemoveGroup = (Boolean)request.getAttribute("admin_remove_button");
boolean bAdminRemoveGroup = (adminRemoveGroup == null ? false : adminRemoveGroup.booleanValue());

Boolean workflowsButton = (Boolean)request.getAttribute("workflows_button");
boolean bWorkflowsButton = (workflowsButton == null ? false : workflowsButton.booleanValue());

Boolean submittersButton = (Boolean)request.getAttribute("submitters_button");
boolean bSubmittersButton = (submittersButton == null ? false : submittersButton.booleanValue());

Boolean readersButton = (Boolean)request.getAttribute("readers_button");
boolean bReadersButton = (readersButton == null ? false : readersButton.booleanValue());

Boolean templateButton = (Boolean)request.getAttribute("template_button");
boolean bTemplateButton = (templateButton == null ? false : templateButton.booleanValue());

Boolean policyButton = (Boolean)request.getAttribute("policy_button");
boolean bPolicyButton = (policyButton == null ? false : policyButton.booleanValue());

Boolean deleteButton = (Boolean)request.getAttribute("delete_button");
boolean bDeleteButton = (deleteButton == null ? false : deleteButton.booleanValue());

// Is the logged in user a sys admin
Boolean admin = (Boolean)request.getAttribute("is.admin");
boolean isAdmin = (admin == null ? false : admin.booleanValue());

HarvestedCollection hc = (HarvestedCollection) request.getAttribute("harvestInstance");

String name = "";
String shortDesc = "";
String intro = "";
String copy = "";
String side = "";
String license = "";
String provenance = "";

String oaiProviderValue= "";
String oaiSetIdValue= "";
String metadataFormatValue= "";
String lastHarvestMsg= "";
int harvestLevelValue=0;
int harvestStatus= 0;

Group[] wfGroups = new Group[3];
wfGroups[0] = null;
wfGroups[1] = null;
wfGroups[2] = null;

Group admins     = null;
Group submitters = null;
Group readers	 = null;
Group workflowGroup = null;

String submitAllChecked = "checked";
String submitRestrictedChecked = "";
String submittersButtonShown = "style=\"display: none\" ";

String readersAllChecked = "checked";
String readersRestrictedChecked = "";
String readersButtonShown = "style=\"display: none\" ";

Item template = null;

/*  Bitstream logo = null; */

String submitGroupsLink = "";
String readerGroupsLink = "";
String workflowGroupsLink =  "";

if (collection != null)
{
  name = collection.getMetadata("name");
  shortDesc = collection.getMetadata("short_description");
  intro = collection.getMetadata("introductory_text");
  copy = collection.getMetadata("copyright_text");
  side = collection.getMetadata("side_bar_text");
  provenance = collection.getMetadata("provenance_description");

  if (collection.hasCustomLicense())
  {
    license = collection.getLicense();
  }

  wfGroups[0] = collection.getWorkflowGroup(1);
  wfGroups[1] = collection.getWorkflowGroup(2);
  wfGroups[2] = collection.getWorkflowGroup(3);

  //        admins     = collection.getAdministrators();
  admins = collection.getPolicyGroup(PolicyType.ADMIN);
  //       submitters = collection.getSubmitters();
  submitters = collection.getPolicyGroup(PolicyType.SUBMIT);
  Group submitTarget = collection.getPolicyTargetGroup(PolicyType.SUBMIT);

  // readers = collection.getReaders();
  readers = collection.getPolicyGroup(PolicyType.READ);
  Group readTarget = collection.getPolicyTargetGroup(PolicyType.READ);

  // Set up checked attributes for the radio buttons. Null or group 0
  // (which is anonymous) means we pre-check All users. Else we pre-check Restricted
  // and display the selected group
  if (submitTarget == null || submitTarget.isAnon()) {
    submitAllChecked = "checked";
    submittersButtonShown = "style=\"display: none\" ";
  } else {
    submitRestrictedChecked = "checked";
    submittersButtonShown = "";
    submitGroupsLink = GlobusWebAppIntegration.getGlobusGroupHrefTag(submitTarget.getName(), 20);
  }

  if (readTarget == null || readTarget.isAnon()) {
    readersAllChecked = "checked";
    readersButtonShown = "style=\"display: none\" ";
  } else {
    readersRestrictedChecked = "checked";
    readersButtonShown = "";
    readerGroupsLink = GlobusWebAppIntegration.getGlobusGroupHrefTag(readTarget.getName(), 20);

  }

  // get a list of all valid configurations (only one should be valid)
  Group[] workflowMembers;

  for (int i = 1; i <= 3; i++) {
    workflowGroup = collection.getWorkflowGroup(i);
    if (workflowGroup != null) {
      workflowMembers = workflowGroup.getMemberGroups();
      if (workflowMembers != null && workflowMembers.length >= 1){
	workflowGroupsLink = GlobusWebAppIntegration.getGlobusGroupHrefTag(workflowMembers[0].getName(), 20);
	break;
      }
    }
  }


  template = collection.getTemplateItem();

  /*
     logo = collection.getLogo();
   */

  /* Harvesting stuff */
  if (hc != null) {
    oaiProviderValue = hc.getOaiSource();
    oaiSetIdValue = hc.getOaiSetId();
    metadataFormatValue = hc.getHarvestMetadataConfig();
    harvestLevelValue = hc.getHarvestType();
    lastHarvestMsg= hc.getHarvestMessage();
    harvestStatus = hc.getHarvestStatus();
  }

}
%>



<dspace:layout style="submission" titlekey="jsp.tools.edit-collection.title"
               locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.administer"
               nocache="true">

 <script type="text/javascript">
        $(document).ready(function() {
          $('#radio_submitters_restricted').change(function() {
            $('#submit_submitters_edit_div').show(100);
          });
          $('#radio_submitters_public').change(function() {
            $('#submit_submitters_edit_div').hide(100);
          });

          $('#radio_readers_restricted').change(function() {
            $('#submit_readers_edit_div').show(100);
          });
          $('#radio_readers_public').change(function() {
            $('#submit_readers_edit_div').hide(100);
          });

          $( '#CollectionInputForm-globus\\.curation\\.curation\\.type').change(function() {
            if ($(this).val() == "None"){
            	$('#curators_select_div').hide(100);
            } else {
            	$('#curators_select_div').show(100);
            	if ($('#curators_select_div_group').text() == ""){
            		$('#curators_select_select_button').val("${selectSelectButtonTitle}");
            	} else {
            		$('#curators_select_select_button').val("${selectChangeButtonTitle}");
            	}
            }
          });
        });
      </script>
<div class="row">
<h3 class="col-md-8">
<%
    if (collection == null)
    {
%>
    <fmt:message key="jsp.tools.edit-collection.heading1"/>
<% } else { %>
    <fmt:message key="jsp.tools.edit-collection.heading2">
        <fmt:param><%= (name != null ? name : collection.getHandle()) %></fmt:param>
    </fmt:message>
<% } %>
	<span>
		<dspace:popup page="/help/site-admin.html#editcollection"><fmt:message key="jsp.help"/></dspace:popup>
	</span>
	</h3>

	<% String termsBanner = Globus.getCollectionConfigurationBannerMsg();
	if (termsBanner != null) {%>
		<div class="col-md-10 alert alert-info"> <%=termsBanner %></div></br>
	<% } %>

    <%-- Add the error display if needed --%>
    <dspace:globuserrordisplay/>

    <% if(bDeleteButton) { %>
              <form class="col-md-4 pull-right" method="post" action="">
                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COLLECTION %>" />
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                <input class="btn btn-danger col-md-12" type="submit" name="submit_delete" value="<fmt:message key="jsp.tools.edit-collection.button.delete"/>" />
              </form>
<% } %>
</div>

<br/>

<div class="row">
<form class="form-group" method="post" action="<%= request.getContextPath() %>/tools/edit-communities">
	<div class="col-md-7">

<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
     <div class="panel panel-primary">
     	<div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.basic-metadata" /></div>
     	<div class="panel-body">
        	<div class="row">
                <label class="col-md-3" for="name"><fmt:message key="jsp.tools.edit-collection.form.label1"/></label>
                <span class="col-md-9">
                	<input class="form-control" type="text" name="name" placeholder="<fmt:message key="jsp.tools.edit-collection.form.plaeholder.name"/>" value="<%= Utils.addEntities(name) %>" />
                </span>
            </div><br/>
            <div class="row">
                <label class="col-md-3" for="short_description"><fmt:message key="jsp.tools.edit-collection.form.label2"/></label>
                <span class="col-md-9">
                	<input class="form-control" type="text" name="short_description" placeholder="<fmt:message key="jsp.tools.edit-collection.form.placeholder.short-description"/>" value="<%= Utils.addEntities(shortDesc) %>" size="50"/>
                </span>
            </div><br/>
            <div class="row">
                <label class="col-md-3" for="introductory_text"><fmt:message key="jsp.tools.edit-collection.form.label3"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="introductory_text" placeholder="<fmt:message key="jsp.tools.edit-collection.form.placeholder.introductory-text"/>" rows="6" cols="50"><%= Utils.addEntities(intro) %></textarea>
                </span>
            </div><br/>
            <%-- Globus Repurposing sidebar as additional information --%>
            <div class="row">
            	<label class="col-md-3" for="side_bar_text"><fmt:message key="jsp.tools.edit-collection.form.label5"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="side_bar_text" rows="6" cols="50" placeholder="<fmt:message key="jsp.tools.edit-collection.form.placeholder.sidebar"/>"><%= Utils.addEntities(side) %></textarea>
                </span>
            </div><br/>

            <div class="row">
            	<label class="col-md-3" for="license"><fmt:message key="jsp.tools.edit-collection.form.label6"/></label>
                <span class="col-md-9">
                	<textarea class="form-control" name="license" placeholder="<fmt:message key="jsp.tools.edit-collection.form.placeholder.license"/>" rows="6" cols="50"><%= Utils.addEntities(license) %></textarea>
                </span>
            </div><br/>

            <div class="row">
                <label class="col-md-3" for="dist-license"><fmt:message key="jsp.tools.edit-collection.form.dist-license-label"/></label>
                <span class="col-md-9">
                    <textarea class="form-control" name="copyright_text" placeholder="<fmt:message key="jsp.tools.edit-collection.form.placeholder.dist-license"/>" rows="6" cols="50"><%= Utils.addEntities(copy) %></textarea>
                </span>
            </div><br/>


		</div>
	</div>
</div>
<div class="col-md-5">


<%-- Globus/identifier config--%>
<dspace:globusstorageconfig collection="<%=collection%>" context="<%=context %>"/>
<dspace:identifierconfig DSpaceObject="<%=objToIntrospect%>" context="<%=context %>" isNewCollection="<%= (collection == null) %>"/>
<dspace:submissionconfig DSpaceObject="<%=objToIntrospect%>" context="<%=context %>"/>


<% if(bSubmittersButton || bWorkflowsButton || bAdminCreateGroup || (admins != null && bAdminRemoveGroup) || bReadersButton) { %>
            <div class="panel panel-default"><div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.label9"/></div>
            <div class="panel-body">

<% }

   if(bSubmittersButton) { %>
<%-- ===========================================================
     Collection Submitters
     =========================================================== --%>
            <div class="row" style="vertical-align: middle">
				<label class="col-md-5" for="submitters_restricted"><fmt:message
					key="jsp.tools.edit-collection.form.label10" /></label>
				<span class="col-md-7 radio">
					<div class="radio">
						<label><input type="radio" name="radio_submitters" value="public"
										id="radio_submitters_public" <%=submitAllChecked%>><fmt:message key="jsp.tools.edit-collection.all.users"/></label>
					</div>
					<div class="radio">
						<label><input type="radio" name="radio_submitters" value="restricted"
										id="radio_submitters_restricted" <%=submitRestrictedChecked %>><fmt:message key="jsp.tools.edit-collection.group.restricted"/></label>
					</div>
					<div id="submit_submitters_edit_div" <%= submittersButtonShown %>>
						<div class="globusGroupLink"><%= submitGroupsLink %></div>
						<input class="btn btn-success col-md-12"
									type="submit" name="submit_submitters_edit" id="submit_submitters_edit"

									<%  if(submitGroupsLink.equals("")) { %>
										value="<fmt:message key="jsp.tools.edit-collection.form.button.select"/>"
									<%  } else { %>
										value="<fmt:message key="jsp.tools.edit-collection.form.button.change"/>"
									<%  }  %>
									/>
					</div>

								<%--
<%  if (submitters == null) {%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_submitters_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  } else { %>
                    <input class="btn btn-default col-md-6"  type="submit" name="submit_submitters_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input class="btn btn-danger col-md-6"  type="submit" name="submit_submitters_delete" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  } %>
--%>
							</span>
						</div><br/>
<%  } %>
<%
   if(bReadersButton) { %>
<%-- ===========================================================
     Collection Readers
     =========================================================== --%>
            <div class="row" style="vertical-align: middle">
                <label class="col-md-5" for="submit_readers_restricted"><fmt:message key="jsp.tools.edit-collection.access.data"/></label>
                <span class="col-md-7 radio">
                  <div class="radio">
                    <label><input type="radio" name="radio_readers" id="radio_readers_public" value="public"
                            <%=readersAllChecked%>><fmt:message key="jsp.tools.edit-collection.all.users"/></label>
                  </div>
                  <div class="radio">
                    <label><input type="radio" name="radio_readers" id="radio_readers_restricted" value="restricted"
                             <%=readersRestrictedChecked%>><fmt:message key="jsp.tools.edit-collection.group.restricted"/></label>
                  </div>
                  <div id="submit_readers_edit_div" <%=readersButtonShown %>>
						<div class="globusGroupLink"><%= readerGroupsLink %></div>
                    <input class="btn btn-success col-md-12" type="submit"
                       name="submit_readers_edit"
                       id="submit_readers_edit"
                       <%  if(readerGroupsLink.equals("")) { %>
							value="<fmt:message key="jsp.tools.edit-collection.form.button.select"/>"
						<%  } else { %>
							value="<fmt:message key="jsp.tools.edit-collection.form.button.change"/>"
						<%  }  %>
                       />
					</div>

                <!--
                   <label>
                     <input type="checkbox" name="submit_readers_public">Public or</input></label>
                    <input class="btn btn-success col-md-6"  type="submit" name="submit_readers_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    -->
<%--
<%  if (readers == null) {%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_readers_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  } else { %>
                    <input class="btn btn-default col-md-6"  type="submit" name="submit_readers_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input class="btn btn-danger col-md-6"  type="submit" name="submit_readers_delete" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  } %>
--%>
				</span>
			</div><br/>
<%  } %>


<% if(bWorkflowsButton) { %>


<%-- Only have one group now, so make this much easier --%>
			<div id="curators_select_div">

    		<div class="row" >
                <label class="col-md-5" for="submit_wf_create_1"><fmt:message key="jsp.tools.edit-collection.form.label11"/></label>
                <span class="col-md-7 btn-group">
                	<div id="curators_select_div_group" class="globusGroupLink"><%=workflowGroupsLink%></div>
                    <input id="curators_select_select_button" class="btn btn-success col-md-12"
                    type="submit" name="submit_wf_edit_1"
                    <%  if(workflowGroupsLink.equals("")) { %>
							value="<fmt:message key="jsp.tools.edit-collection.form.button.select"/>"
					<%  } else { %>
							value="<fmt:message key="jsp.tools.edit-collection.form.button.change"/>"
					<%  }  %>
                    />
				</span>
			</div>
			</div>
			<br/>


<%  } %>
<% if(false && (bAdminCreateGroup || (admins != null && bAdminRemoveGroup))) { %>
<%-- ===========================================================
     Collection Administrators
     =========================================================== --%>
            <div class="row">
                <label class="col-md-6" for="submit_admins_create"><fmt:message key="jsp.tools.edit-collection.form.label12"/></label>
                <span class="col-md-6 btn-group">
<%  if (admins == null) {
		if (bAdminCreateGroup) {
%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_admins_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  	}
	}
	else {
		if (bAdminCreateGroup) {
	%>
                    <input class="btn btn-default" type="submit" name="submit_admins_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
	<%  }
		if (bAdminRemoveGroup) {
		%>
                    <input class="btn btn-danger" type="submit" name="submit_admins_delete" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  	}
	}	%>
				</span>
			</div>
		</div>
	</div>
<% } else { %>
		</div>
	</div>
	<br/>
<% } %>

<%-- If for entire panel is a good idea, false hack not to show panel is not so good of an idea --%>
<% if (false && (bTemplateButton || bPolicyButton || bAdminCollection)) { %>
<div class="panel panel-default">
	<div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.collection-settings" /></div>
	<div class="panel-body">
<% 	if(bTemplateButton) { %>
			<div class="row">
<!-- ===========================================================
     Item template
     =========================================================== -->
                <label class="col-md-6" for="submit_create_template"><fmt:message key="jsp.tools.edit-collection.form.label13"/></label>
                <span class="col-md-6 btn-group">
<%  	if (template == null) {%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_create_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />

<%  	} else { %>
                    <input class="btn btn-default col-md-6" type="submit" name="submit_edit_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input class="btn btn-default col-md-6" type="submit" name="submit_delete_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  	} %>
				</span>
			</div><br/>
<%  } %>

<% 	if(bPolicyButton) { %>
<!-- ===========================================================
     Edit collection's policies
     =========================================================== -->
     		<div class="row">
                <label class="col-md-6" for="submit_authorization_edit"><fmt:message key="jsp.tools.edit-collection.form.label14"/></label>
                <span class="col-md-6 btn-group">
                    <input class="btn btn-success col-md-12" type="submit" name="submit_authorization_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                </span>
        	</div><br/>
<%  } %>

<% 	if(bAdminCollection) { %>
<!-- ===========================================================
     Curate collection
     =========================================================== -->
            <div class="row">
                <label class="col-md-6" for=""><fmt:message key="jsp.tools.edit-collection.form.label27"/></label>
                <span  class="col-md-6 btn-group">
                    <input class="btn btn-success col-md-12" type="submit" name="submit_curate_collection" value="<fmt:message key="jsp.tools.edit-collection.form.button.curate"/>" />
				</span>
			</div>
<%  } %>

		</div>
   </div>
<% } //If around whole panel depending on if any of the options are enabled %>
   <%-- Quick hack to remove this from the UX --%>
<% if(false && bAdminCollection) { %>
<%-- ===========================================================
     Harvesting Settings
     =========================================================== --%>
   <div class="panel panel-default">
       	<div class="panel-heading"><fmt:message key="jsp.tools.edit-collection.form.label15"/></div>
		<div class="panel-body">

     		<%--
     		oaiProviderValue = hc.getOaiSource();
			oaiSetIdValue = hc.getOaiSetId();
			metadataFormatValue = hc.getHarvestMetadataConfig();
			harvestLevelValue = hc.getHarvestType();
			String lastHarvestMsg= hc.getHarvestMessage();
			int harvestStatus = hc.getHarvestStatus();

			if (lastHarvestMsg == null)
				lastHarvestMsg = "none";
			--%>

                <div class="input-group">
                <label class="input-group-addon" for="source_normal"><fmt:message key="jsp.tools.edit-collection.form.label16"/></label>
                <div class="form-control">
                	<input class="col-md-1" type="radio" value="source_normal" <% if (harvestLevelValue == 0) { %> checked="checked" <% } %> name="source" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label17"/></span>
               		<input class="col-md-1" type="radio" value="source_harvested" <% if (harvestLevelValue > 0) { %> checked="checked" <% } %> name="source" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label18"/></span>
                </div>
                </div><br/>
                <div class="row">
                	<label class="col-md-3" for="oai_provider"><fmt:message key="jsp.tools.edit-collection.form.label19"/></label>
                	<span class="col-md-9">
                		<input class="form-control" type="text" name="oai_provider" value="<%= oaiProviderValue %>" size="50" />
                	</span>
                </div><br/>
                <div class="row">
                	<label class="col-md-3" for="oai_setid"><fmt:message key="jsp.tools.edit-collection.form.label20"/></label>
                	<span class="col-md-9">
                		<input class="form-control" type="text" name="oai_setid" value="<%= oaiSetIdValue %>" size="50" />
                	</span>
                </div><br/>
                <div class="row">
                	<label class="col-md-3" for="metadata_format"><fmt:message key="jsp.tools.edit-collection.form.label21"/></label>
                	<span class="col-md-9">
                	<select class="form-control" name="metadata_format" >
	                	<%
		                // Add an entry for each instance of ingestion crosswalks configured for harvesting
			            String metaString = "harvester.oai.metadataformats.";
			            Enumeration pe = ConfigurationManager.propertyNames("oai");
			            while (pe.hasMoreElements())
			            {
			                String key = (String)pe.nextElement();


			                if (key.startsWith(metaString)) {
			                	String metadataString = ConfigurationManager.getProperty("oai", key);
			                	String metadataKey = key.substring(metaString.length());
								String label = "jsp.tools.edit-collection.form.label21.select." + metadataKey;

	                	%>
			                	<option value="<%= metadataKey %>"
			                	<% if(metadataKey.equalsIgnoreCase(metadataFormatValue)) { %>
			                	selected="selected" <% } %> >
								<fmt:message key="<%=label%>"/>
								</option>
			                	<%
			                }
			            }
		                %>
					</select>
					</span>
				</div><br/>
				<div class="input-group">
                <label class="input-group-addon" for="harvest_level"><fmt:message key="jsp.tools.edit-collection.form.label22"/></label>
                <div class="form-control">
                	<input class="col-md-1" type="radio" value="1" <% if (harvestLevelValue != 2 && harvestLevelValue != 3) { %> checked="checked" <% } %> name="harvest_level" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label23"/></span><br/>
                	<input class="col-md-1" type="radio" value="2" <% if (harvestLevelValue == 2) { %> checked="checked" <% } %> name="harvest_level" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label24"/></span><br/>
                	<input class="col-md-1" type="radio" value="3" <% if (harvestLevelValue == 3) { %> checked="checked" <% } %> name="harvest_level" />
                	<span class="col-md-11"><fmt:message key="jsp.tools.edit-collection.form.label25"/></span><br/>
                </div>
                </div><br/>
                <div class="row">
                <label class="col-md-6"><fmt:message key="jsp.tools.edit-collection.form.label26"/></label>
                <span class="col-md-6"><%= lastHarvestMsg %></span>
                </div>
		</div>
	</div>
<%  } %>
</div>
<div class="btn-group col-md-12">
<%
	// Do this based on name since we always get a collection now
    if (name == null || "".equals(name))
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="create" value="true" />
                        <input class="btn btn-success col-md-6" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.create2"/>" />
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                        <input type="hidden" name="create" value="false" />
                        <input class="btn btn-success col-md-6" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.update"/>" />
<%
    }
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COLLECTION %>" />
                        <input class="btn btn-warning col-md-6" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-collection.form.button.cancel"/>" />
</div>
    </form>
    </div>
</dspace:layout>
