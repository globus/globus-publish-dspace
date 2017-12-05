/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.app.webui.jsptag;

import static org.dspace.globus.GlobusHtmlUtil.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.globus.Globus;
import org.dspace.globus.configuration.BootstrapFormRenderer;
import org.dspace.globus.configuration.Configurable;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.transfer.Endpoint;

/**
 *
 */
public class GlobusStorageConfigTag extends TagSupport
{
    /**
     *
     */
    private static final String BR_TAG = "<br/>";

    private static final Logger logger = Logger.getLogger(GlobusStorageConfigTag.class);

    public static final String TEST_STORAGE_BUTTON_NAME = "submit_test_storage";

    public static final String TEST_STATUS_REQ_ATTR = "GlobusStorageConfig.test_status";

    private static final long serialVersionUID = 1L;

    private Collection collection;
    private Context context;


    public void setCollection(Collection collection)
    {
        this.collection = collection;
    }


    public void setContext(Context context)
    {
        this.context = context;
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

        Configurable storageConfig = Globus.getGlobusStorageCollectionConfig();

        try {
            BootstrapFormRenderer bfr = new BootstrapFormRenderer(5, 7);
            StringBuffer providerPanels = new StringBuffer();
            String rendering = (String) bfr.renderFrom(storageConfig, context, collection);
            if (rendering != null) {
                providerPanels.append(rendering);
            }

            providerPanels.append(br());

            ServletRequest request = pageContext.getRequest();
            Object testStatusObj = request.getAttribute(TEST_STATUS_REQ_ATTR);
            String testStatusMsg = "";
            if (testStatusObj != null) {
                String testStatus = testStatusObj.toString();
                if ("".equals(testStatus)) {
                    testStatusMsg = div("class=alert alert-success", "Test Successful");
                } else {
                    testStatusMsg = div("class=alert alert-danger", testStatusObj.toString());
                }
                providerPanels.append(testStatusMsg);
                request.removeAttribute(TEST_STATUS_REQ_ATTR);
            }

            String testButton =
                input("class=btn col-md-12 btn-default, type=submit, name="
                    + TEST_STORAGE_BUTTON_NAME + ", value=Test Setting");

            providerPanels.append(testButton);

            writer.append(div("class=panel panel-default",
                              div("class=panel-heading", "Dataset Storage Settings"),
                              div("class=panel-body", providerPanels.toString())));
            writer.append("\n");

        } catch (IOException e) {
            logger.error("Failed to render StorageConfigTag", e);
        }
        return SKIP_BODY;
    }


    public static String validateInputConfig(BootstrapFormRenderer bfr, HttpServletRequest request)
    {
        String epName =
            bfr.getConfigValueFromValueRep(request, Globus.getGlobusStorageCollectionConfig(),
                                           Globus.CONFIG_GLOBUS_ENDPOINT_NAME);
        if (epName == null) {
            return null;
        }
        GlobusClient pc = Globus.getPrivlegedClient();
        if (pc != null) {
            try {
                Endpoint endpoint = pc.getEndpoint(epName);
                if (endpoint.s3Url != null) {
                    return "Collection endpoint must not reside on S3";
                }
            } catch (GlobusClientException gce) {
                String message = gce.getMessage();
                return "Failed communicating with endpoint " + epName + " due to " + message;
            }
        }
        return null;
    }

    /**
     * @param request
     * @param respo
     * @throws GlobusClientException
     */
    public static void testInputConfig(BootstrapFormRenderer bfr, HttpServletRequest request)
    {
        String epName =
            bfr.getConfigValueFromValueRep(request, Globus.getGlobusStorageCollectionConfig(),
                                           Globus.CONFIG_GLOBUS_ENDPOINT_NAME);
        String pathName =
            bfr.getConfigValueFromValueRep(request, Globus.getGlobusStorageCollectionConfig(),
                                           Globus.CONFIG_GLOBUS_SHARE_PATH_PREFIX);
        String publishUserSubstitutionString = "$publishUser";
        String privUserName = ConfigurationManager.getProperty(Globus.GLOBUS_AUTH_CONFIG_MODULE,
                                                               Globus.GLOBUS_USER_CONFIG_PROP);
        if (privUserName == null) {
            privUserName = "Globus Auth identity for the service";
        }
        

        if (epName == null || pathName == null) {
            request.setAttribute(TEST_STATUS_REQ_ATTR,
                                 "Shared Storage Endpoint and Dataset Path Prefix are required");
            return;
        }
        pathName = pathName + "_EndPointTest_" + System.currentTimeMillis();
        // We are helping the user out but putting a slash at the beginning if they haven't
        if (!pathName.startsWith("/")) {
            pathName = "/" + pathName;
        }
        GlobusClient gc = Globus.getPrivlegedClient();
        List<String> testErrors = new ArrayList<String>();;
        try {
            gc.activateEndpoint(epName);
            Endpoint epDef = gc.getEndpoint(epName);
            String[] roles = epDef.myEffectiveRoles;
            boolean foundManager = false;
            if (roles != null) {
                for (String role : roles) {
                    logger.info("Got role: " + role);
                    if ("access_manager".equals(role)) {
                        foundManager = true;
                        break;
                    }
                }
            }
            if (!foundManager) {
                testErrors.add("Globus user '" + publishUserSubstitutionString
                    + "' must have rights to set ACLs on the endpoint");
            }
            try {
                gc.createDirectory(epName, pathName);
                gc.deletePath(epName, pathName, true);
            } catch (GlobusClientException gce) {
                testErrors.add("Globus user '" + publishUserSubstitutionString
                    + "' must have rights to create directories on the endpoint");
            }

        } catch (GlobusClientException gce) {
            String message = gce.getMessage();
            if (message != null) {
                testErrors.add(message.replace(publishUserSubstitutionString, privUserName));
            } else {
                testErrors.add("Endpoint test failed unexpectedly, reset settings and try again");
            }
        }
        String testResult = "";
        if (testErrors.size() > 0) {
            StringBuffer sb = new StringBuffer();
            sb.append("<ul>\n");
            for (String msg : testErrors) {
                sb.append("\t<li>")
                    .append(msg)
                    .append("</li>\n");
            }
            sb.append("</ul>\n");
            testResult = sb.toString();
        }
        request.setAttribute(TEST_STATUS_REQ_ATTR, testResult);
    }
}
