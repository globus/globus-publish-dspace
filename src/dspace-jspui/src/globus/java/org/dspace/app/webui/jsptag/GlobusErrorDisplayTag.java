/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.app.webui.jsptag;

import org.apache.log4j.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class GlobusErrorDisplayTag extends TagSupport
{
    private static final Logger logger = Logger.getLogger(GlobusErrorDisplayTag.class);

    public static final String MISSING_INPUT_ATTR = "org.dspace.app.webui.jsptag"
        + ".GlobusErroDisplayTag.missingInput";

    public static final String ERROR_MSG_ATTR = "org.dspace.app.webui.jsptag"
        + ".GlobusErroDisplayTag.errorMsg";


    @Override
    public int doStartTag() throws JspException
    {
        ServletRequest request = pageContext.getRequest();
        Set<String> missingInputs = (Set<String>) request.getAttribute(MISSING_INPUT_ATTR);
        // Once we get this, clear it so that we re-set it if needed on the next page load
        request.removeAttribute(MISSING_INPUT_ATTR);
        String errorMsg = (String) request.getAttribute(ERROR_MSG_ATTR);
        request.removeAttribute(ERROR_MSG_ATTR);

        if (missingInputs == null && errorMsg == null) {
            return SKIP_BODY;
        }

        JspWriter out = pageContext.getOut();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<div class='alert alert-danger col-md-8'>");

        if (errorMsg != null) {
            stringBuffer.append(errorMsg).append("\n");
        }

        if (missingInputs != null && missingInputs.size() > 0) {
            if (errorMsg != null) {
                stringBuffer.append("<br/>\n");
            }
            String fields = "fields";
            String are = "are";
            String values = "values";
            if (missingInputs.size() == 1) {
                fields = "field";
                are = "is";
                values = "a value";
            }
            stringBuffer.append("The following ").append(fields).append(" ").append(are)
                .append(" missing ").append(values).append(":<br/>\n");
            stringBuffer.append("<ul>");
            for (String inputName : missingInputs) {
                stringBuffer.append("<li>").append(inputName).append("</li>\n");
            }
            stringBuffer.append("</ul>");
        }

        stringBuffer.append("</div>");
        try {
            out.write(stringBuffer.toString());
        } catch (IOException e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }


    public static void addErrorMsg(ServletRequest request, String message)
    {
        if (message != null) {
            Object oldMsgObj = request.getAttribute(ERROR_MSG_ATTR);
            if (oldMsgObj != null) {
                message = oldMsgObj.toString() + "<br/>\n" + message;
            }
            request.setAttribute(ERROR_MSG_ATTR, message);
        }
    }


    public static void addMissingInput(ServletRequest request, String fieldName)
    {
        addAllMissingInputs(request, Collections.singleton(fieldName));
    }


    public synchronized static void addAllMissingInputs(ServletRequest request,
                                                        Collection<String> allFields)
    {
        if (allFields == null || allFields.size() == 0) {
            return;
        }
        Set<String> missing = (Set<String>) request.getAttribute(MISSING_INPUT_ATTR);
        if (missing == null) {
            missing = new HashSet<String>();
            request.setAttribute(MISSING_INPUT_ATTR, missing);
        }
        missing.addAll(allFields);
    }


    public static boolean hasError(ServletRequest request)
    {
        if (request.getAttribute(ERROR_MSG_ATTR) != null) {
            return true;
        }
        if (request.getAttribute(MISSING_INPUT_ATTR) != null) {
            return true;
        }
        return false;
    }
}
