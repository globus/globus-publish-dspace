/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */


package org.dspace.app.webui.jsptag;

import static org.dspace.globus.GlobusHtmlUtil.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.globus.configuration.BootstrapFormRenderer;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.Configurable.ConfigurableProperty;
import org.dspace.globus.configuration.GlobusConfigurationManager;
import org.dspace.globus.identifier.GlobusIdentifierService;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

/**
 *
 */
public class IdentifierConfigTag extends TagSupport
{
    /**
     *
     */
    private static final String OPTION_CLASS_INUSE = "inuse";

    /**
     *
     */
    private static final String OPTION_CLASS_PID_CREATION = "creator";

    /**
     *
     */
    private static final String OPTION_CLASS_FROM_PARENT = "fromparent";

    private static final Logger log = Logger.getLogger(IdentifierConfigTag.class);

    private static final long serialVersionUID = 1L;

    private static final String CONTROL_SCRIPT_TAG = "<script type=\"text/javascript\" src=\"identifier-config.js\"/>";

    private static final String CONTROL_SCRIPT = "<script type=\"text/javascript\">\n" +
    "$( document ).ready(function() {\n" +
    "    var providerDivs = [{{providerList}}];\n" +
    "    var cur = '#{{currentProvider}}';\n" +
    "    var inUse = { {{inUseList}} };\n" +
    "    var creationDivs = [{{creationList}}];\n" +
    "\n" +
    "    function configureControls(newCur) {\n" +
    "    $( cur ).hide(250);\n" +
    "    cur = newCur;\n" +
    "    $( cur ).show(250);\n" +
    "    if ($.inArray(cur, creationDivs) != -1) {\n" +
    "        $( '#submit_delete_id' ).hide();\n" +
    "        $( '#submit_create_id' ).show();\n" +
    "    } else {\n" +
    "        $( '#submit_delete_id' ).show();\n" +
    "        $( '#submit_create_id' ).hide();\n" +
    "    }\n" +
    "    var newTitle = '';\n" +
    "    var newDisabled = false;\n" +
    "    if (inUse.hasOwnProperty(cur)) {\n" +
    "        newDisabled = true;\n" +
    "        newTitle = inUse[cur];\n" +
    "    }\n" +
    "    $( '#submit_delete_id' ).prop('disabled', newDisabled);\n" +
    "    $( '#submit_delete_id_div' ).prop('title', newTitle);    \n" +
    "    }\n" +
    "    \n" +
    "    for (var i = 0; i < providerDivs.length; i++) {\n" +
    "        $( providerDivs[i] ).hide();\n" +
    "    }\n" +
    "\n" +
    "    configureControls( cur );\n" +
    "\n" +
    "    $( '#provider-selector' ).change(function () {\n" +
    "        var newSel = '#' + this.value;\n" +
    "    configureControls( newSel );\n" +
    "    });\n" +
    "});" +
        "</script>\n";

    public static final String IDENTIFIER_DELETE_BUTTON_ID = "submit_delete_id";
    public static final String IDENTIFIER_CREATE_BUTTON_ID = "submit_create_id";

    private DSpaceObject dso;
    private Context context;
    private boolean isNewCollection = false;


    public void setDSpaceObject(DSpaceObject dso)
    {
        this.dso = dso;
    }


    public void setContext(Context context)
    {
        this.context = context;
    }

    public void setIsNewCollection(boolean val)
    {
        this.isNewCollection = val;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException
    {
        JspWriter writer = pageContext.getOut();

        IdentifierService idSvc = new DSpace().getSingletonService(IdentifierService.class);
        List<Configurable> providerConfigs = null;
        String currentProviderId = GlobusIdentifierService.DEFAULT_PROVIDER_NAME;
        if (idSvc instanceof GlobusIdentifierService) {
            GlobusIdentifierService globusIdSvc = (GlobusIdentifierService) idSvc;
            providerConfigs = globusIdSvc.getProviderConfigurations(context, dso);
            String configGroup =
                GlobusIdentifierService.getIdentifierConfigNameForDso(context, dso);
            // Get the current provider so we can initialize it
            if (configGroup != null) {
                currentProviderId = configGroup;
            }
        }

        // If we cannot get the list of providers for the id svc, there's no point in doing the
        // configuration of them
        if (providerConfigs == null || providerConfigs.size() == 0) {
            return SKIP_BODY;
        }

        StringBuffer optionTags = new StringBuffer();
        for (Configurable providerConfig : providerConfigs) {
            String providerName = providerConfig.getName();
            String displayName = providerName;
            String providerDivId = BootstrapFormRenderer.divIdForConfig(providerConfig);
            ConfigurableProperty configName =
                providerConfig.getProperty(IdentifierProvider.IDENTIFIER_CONFIGURATION_CFG_NAME);
            String providerTagClass = "";
            if (configName != null) {
                displayName = "New Custom " + displayName;
                providerTagClass = OPTION_CLASS_PID_CREATION;
            } else {
                String providerImpl =
                    GlobusIdentifierService.getProviderNameForConfigName(context, dso, displayName);
                if (isNewCollection) {
                    providerTagClass = OPTION_CLASS_FROM_PARENT;
                } else {
                    String cantDeleteReason = canDeleteProvider(displayName, context, dso);
                    if (cantDeleteReason != null) {
                        providerTagClass = cantDeleteReason;
                    }
                }
                // We want to make sure that the type of the identifier is being displayed. If the
                // user provided name contains the name, then we present that name. If not, we
                // append the type of the identifier after a : seperator
                if (providerImpl != null
                    && !displayName.toLowerCase().contains(providerImpl.toLowerCase())) {
                    displayName = displayName + " : " + providerImpl;
                }
                // We only have to check for delete-able config. if it is a configured config.
                // not an option to create Custom
            }
            // If this one is selected, setup to add that attribute to the tag
            String selected = "";
            if (providerName.equals(currentProviderId)) {
                selected = ",selected";
            }
            if (!"".equals(providerTagClass)) {
                providerTagClass = ",class=" + providerTagClass;
            }
            optionTags.append(tag("option", "value=" + providerDivId + selected + providerTagClass,
                                  displayName));
        }
        // providerNameList.append("];\n");
        try {
            BootstrapFormRenderer bfr = new BootstrapFormRenderer(5, 7);
            StringBuffer providerPanels = new StringBuffer();
            for (int i = 0; i < providerConfigs.size(); i++) {
                // Pass null instead of the DSO so we only populate the form with default values
                // Not values used in a previous ID creation
                String rendering = (String) bfr.renderFrom(providerConfigs.get(i), context, null);
                if (rendering != null) {
                    providerPanels.append(rendering);
                }
            }
            writer
                .append(div("class=panel panel-default",
                            div("class=panel-heading", "Identifier Settings"),
                            div("class=panel-body",
                                div("class=row",
                                    label("class=col-md-5, for=side_bar_text", "Configuration"),
                                    span("class=col-md-7",
                                         tag("select",
                                             "class=form-control, name=provider-selector,id=provider-selector",
                                             optionTags.toString()))),
                                "<br/>",
                                providerPanels.toString(),
                                div("class=col-md10 text-center, id=" + IDENTIFIER_DELETE_BUTTON_ID
                                        + "_div",
                                    input("name="
                                        + IDENTIFIER_DELETE_BUTTON_ID
                                        + ",id="
                                        + IDENTIFIER_DELETE_BUTTON_ID
                                        + ",class=btn col-md-12 btn-danger, type=submit, value=Delete Configuration"),
                                    input("name="
                                        + IDENTIFIER_CREATE_BUTTON_ID
                                        + ",id="
                                        + IDENTIFIER_CREATE_BUTTON_ID
                                        + ",class=btn col-md-12 btn-success, type=submit, value=Create")))));

            writer.append("\n<br/>\n");
            writer.append("\n");

            String contextPath = pageContext.getServletContext().getContextPath();
            log.info("Context Path: " + contextPath);
            ServletRequest servletRequest = pageContext.getRequest();
            if (servletRequest instanceof HttpServletRequest) {
                HttpServletRequest httpReq = (HttpServletRequest) servletRequest;
                log.info("Http Request: " + httpReq);
            }

            String scriptTag = tag("script", true, "src="+contextPath+ "/static/js/identifier-config.js, type=text/javascript");
            writer.append(scriptTag);
            writer.append("</script>");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return SKIP_BODY;
    }


    /**
     * Test whether a particular identifier provider configuration can be deleted with respect to a
     * particular DSpaceObject. If it is in use by a child of the provided DSpaceObject, or was not
     * initially defined on this DSpaceObject it cannot be deleted.
     * @param providerName The name of the provider configuration.
     * @param context The context used for accessing the database.
     * @param dso The DSpaceObject to test against.
     * @return {@code null} if it can be removed or a string message indicating the reason it cannot
     *         be deleted.
     */
    private static String canDeleteProvider(String providerName, Context context, DSpaceObject dso)
    {
        // First, check if this provider is defined on this DSpaceObject
        String propertyVal =
            GlobusConfigurationManager
                .getPropertyOnDso(context, IdentifierProvider.IDENTIFIER_CONFIGURATION_CFG_NAME,
                                  providerName, dso);
        if (propertyVal == null) {
            // Not defined here, so cannot be deleted here
            return OPTION_CLASS_FROM_PARENT;
        }
        // If it is, it was defined locally, so may be eligible to be deleted here if it is not
        // being used by any child instances
        if (propertyVal.equals(providerName)) {
            // Collections have no children, so this is ok
            if (dso instanceof Collection) {
                return null;
            } else if (dso instanceof Community) {
                Community community = (Community) dso;
                try {
                    Collection[] allCollections = community.getAllCollections();
                    if (allCollections != null) {
                        for (Collection collection : allCollections) {
                            // This will get the provider as defined explicitly on this collection
                            // without walking up the hierarchy
                            String providerForColl =
                                GlobusConfigurationManager
                                    .getPropertyOnDso(context,
                                                      GlobusIdentifierService.CFG_IDENTIFIER_NAME,
                                                      null, collection);
                            if (providerForColl != null && providerForColl.equals(providerName)) {
                                return OPTION_CLASS_INUSE;
                            }
                        }
                    }
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public static String getSelectedProvider(HttpServletRequest request)
    {
        String selected = request.getParameter("provider-selector");
        selected = BootstrapFormRenderer.configNameForDivId(selected);
        return selected;
    }


    /**
     * @param request
     * @param collection
     * @param context
     */
    public static void deleteProvider(HttpServletRequest request, DSpaceObject dso,
                                      Context context)
    {
        String provider = getSelectedProvider(request);
        if (provider == null) {
            return;
        }
        if (canDeleteProvider(provider, context, dso) == null) {
            GlobusConfigurationManager.removeConfigGroup(context, provider);
        }
    }
}
