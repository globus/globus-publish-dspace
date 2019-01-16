/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.servlet.SubmissionController;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.globus.Globus;
import org.dspace.globus.GlobusUIUtil;
import org.dspace.globus.GlobusWebAppIntegration;
import org.globus.GlobusClient;
import org.globus.transfer.Task;

/**
 *
 */
public class GlobusTransferStatusTag extends TagSupport
{
    private static final Logger logger = Logger.getLogger(GlobusTransferStatusTag.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String activeheader = null;
    private String inactiveheader = null;

    private boolean metadata = false;

    private boolean summaryOnly = false;



    public GlobusTransferStatusTag()
    {
        super();
    }


    public void setMetadata(String metadataValue)
    {
        if (metadataValue != null) {
            this.metadata = Boolean.valueOf(metadataValue);
        }
    }


    public void setSummaryOnly(String summaryValue)
    {
        if (summaryValue != null) {
            summaryOnly = Boolean.valueOf(summaryValue);
        }
    }

    public void setActiveheader(String activeheader)
    {
        this.activeheader = activeheader;
    }

    public void setInactiveheader(String inactiveheader)
    {
        this.inactiveheader = inactiveheader;
    }

    public static List<Task> getTasksForRequest(Context context, HttpServletRequest request,
                                                boolean asUser) throws SQLException,
                                                    ServletException
    {
        if (context == null) {
            context = UIUtil.obtainContext(request);
        }
        List<Task> tasks = null;
        GlobusClient gc = null;
        if (asUser) {
            Globus g = Globus.getGlobusClientFromRequest(request);
            if (g == null) {
                g = Globus.getGlobusClientFromContext(context);
            }
            if (g == null) {
                logger.error("Cannot get Globus Client for request:" + request);
                return null;
            }
            gc = g.getClient();
        } else {
            gc = Globus.getPrivlegedClient();
        }
        SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
        if (subInfo != null) {
            InProgressSubmission subItem = subInfo.getSubmissionItem();
            if (subItem != null) {
                Item item = subItem.getItem();
                if (item != null) {
                    String epName = item.getGlobusEndpoint();
                    String sharePath = item.getGlobusSharePath();
                    if (epName != null && sharePath != null) {
                        tasks =
                            GlobusUIUtil.getTasksForDestEndpoint(request, gc, epName, sharePath,
                                                                 asUser);
                    }
                }
            }
        }
        return tasks;
    }


    /**
     * Check whether all transfers for the current item submission are complete. This may involve
     * fetching the entire transfer task list for this user/endpoint.
     * @param request Request object for the web transaction we're checking on
     * @param asUser True if we want to know about transfers performed by the user, False if by the
     *            Globus Publish user (for artifact creation)
     * @return True if all transfers are complete.
     */
    public static boolean allTransfersComplete(HttpServletRequest request, boolean asUser)
    {
        Object allDoneObj = request.getAttribute(GlobusUIUtil.TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE);
        if (allDoneObj == null) {
            try {
                getTasksForRequest(null, request, asUser);
                allDoneObj =
                    request.getAttribute(GlobusUIUtil.TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE);
            } catch (Exception e) {
                logger.error("Failed to get task list due to ", e);
                return true;
            }
        }
        return allDoneObj != null && ((Boolean) allDoneObj);
    }


    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException
    {
        // No artifact or artifact with False value indicates we're doing this for the user
        ServletRequest request = super.pageContext.getRequest();
        JspWriter out = super.pageContext.getOut();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            try {
                StringBuffer buf = new StringBuffer();
                List<Task> tasksForRequest = getTasksForRequest(null, httpRequest, !metadata);
                if (!summaryOnly) {
                    if (tasksForRequest != null && tasksForRequest.size() > 0) {
                        if (activeheader != null) {
                            out.write("<h3>"+activeheader+"</h3>\n");
                        }
                        appendTaskTable(buf, httpRequest, tasksForRequest, !metadata);
                        out.write(buf.toString());
                    } else {
                        if (inactiveheader != null) {
                            out.write("<h3>"+inactiveheader+"</h3>\n");
                        }
                    }
                } else {
                    boolean allTasksComplete =
                        GlobusUIUtil.allTasksComplete(httpRequest, !metadata);
                    boolean anyFailures = GlobusUIUtil.anyTaskFailures(httpRequest, !metadata);
                    if (allTasksComplete) {
                        out.write("Completed");
                    } else if (anyFailures) {
                        out.write("Errors Encountered");
                    } else {
                        out.write("In progress");
                    }

                }
            } catch (Exception e) {
                logger.warn("Failed getting transfer tasks for request " + httpRequest, e);
                try {
                    out.write("Unable to retrieve transfer status");
                } catch (IOException e1) {
                    logger.warn("Error writing results to JSP page: ", e1);
                }
            }
        }
        return SKIP_BODY;

    }


    /**
     * @param out
     * @param tasksForRequest
     * @throws IOException
     */
    private void appendTaskTable(StringBuffer buf, HttpServletRequest request, List<Task> taskList,
                                 boolean asUser) throws IOException
    {
        buf.append("<div id=\"GlobusTransferTable\">");

        String[] evenOdd = { "evenRow", "oddRow" };
        if (taskList != null && taskList.size() > 0) {
            Locale locale = request.getLocale();
            DateFormat dateFormat =
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);

            int evenOddCounter = 0;
            buf.append("<table class=\"table\" align=\"center\" summary=\"Table listing unfinished submissions\">");
            buf.append("<tr>");
            List<String> headings = new ArrayList<String>();
            headings.add("Status");
            if (asUser) {
                headings.add("Source Endpoint");
            }
            headings.add("Start Time");
            headings.add("Completion Time");
            headings.add("Bytes Transferred");
            appendRow(buf, "EvenRow", headings);

            for (int i = 0; i < taskList.size(); i++) {
                Task task = taskList.get(i);
                String rowEvenOdd = evenOdd[(i + 1) % 2];
                appendTaskRow(buf, rowEvenOdd, dateFormat, task, asUser);
            }
            buf.append("</table>");
            buf.append("</div>");
        }
    }


    private void appendTaskRow(StringBuffer buf, String rowEvenOdd, DateFormat dateFormat,
                               Task task, boolean asUser)
    {
        String manageUrl = GlobusWebAppIntegration.getWebAppActivityUrl(task.taskId);
        List<String> cells = new ArrayList<String>();
        cells.add("<a target=\"GlobusOps\" href=\"" + manageUrl + "\">" + task.status + "</a>");
        if (asUser) {
            cells.add(task.sourceEndpoint);
        }
        cells.add(dateFormat.format(task.requestTime));
        cells.add((task.completionTime != null ? dateFormat.format(task.completionTime) : "---"));
        cells.add(String.valueOf(task.bytesTransferred));
        appendRow(buf, rowEvenOdd, cells);
    }


    private void appendRow(StringBuffer buf, String rowEvenOdd, List<String> cells)
    {
        buf.append("<tr>");
        String[] colNames = { "OddCol", "EvenCol" };
        for (int i = 0; i < cells.size(); i++) {
            String colEvenOdd = colNames[i % colNames.length];
            appendTableCell(buf, rowEvenOdd, colEvenOdd, cells.get(i));
        }
        buf.append("</tr>");
    }


    private void appendTableCell(StringBuffer buf, String rowEvenOdd, String columnEvenOdd,
                                 String content)
    {
        buf.append("<td class=\"" + rowEvenOdd + columnEvenOdd + "\">" + content + "</td>");
    }

}
