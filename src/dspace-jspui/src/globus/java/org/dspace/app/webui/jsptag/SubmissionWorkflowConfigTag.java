/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */


package org.dspace.app.webui.jsptag;

import static org.dspace.globus.GlobusHtmlUtil.*;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.globus.Globus;
import org.dspace.globus.configuration.BaseConfigurable;
import org.dspace.globus.configuration.BootstrapFormRenderer;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.DirectoryListConfigurablePropertyValueOptions;
import org.dspace.globus.configuration.GlobusConfigurationManager;
import org.dspace.globus.configuration.Configurable.ConfigurableProperty;
import org.dspace.globus.configuration.Configurable.DataType;

/**
 *
 */
@Deprecated
public class SubmissionWorkflowConfigTag extends TagSupport
{

    private static final long serialVersionUID = 1L;

    private Collection collection;
    private Context context;

    /**
     *
     */
    private static final ConfigurableProperty WORKFLOW_FORM_CONFIG_PROP =
        new ConfigurableProperty(Globus.CONFIG_SUBMISSION_WORKFLOW, "Submission Workflow",
                                 "item-submission.xml", DataType.STRING, "", false);

    public static final Configurable configuration =
        new BaseConfigurable("Collection Submission Workflow", WORKFLOW_FORM_CONFIG_PROP);


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

        try {
            BootstrapFormRenderer bfr = new BootstrapFormRenderer(5, 7);
            StringBuffer panelFields = new StringBuffer();
            String rendering = (String) bfr.renderFrom(getConfiguration(), context, collection);
            if (rendering != null) {
                panelFields.append(rendering);
            }

            writer.append(div("class=panel panel-default",
                              div("class=panel-heading",
                                  "Submission Workflow Configuration Settings"),
                              div("class=panel-body", panelFields.toString())));
            writer.append("\n");

        } catch (IOException e) {
            throw new JspException("Failed created the submission workflow configuration panel", e);
        }
        return SKIP_BODY;
    }


    /**
     * Doing this in a non-static way so that we can read the configuration needed for setting the
     * location of the forms directory. Syncronized so we can just init it one time.
     * @return
     */
    private synchronized Configurable getConfiguration()
    {
        if (WORKFLOW_FORM_CONFIG_PROP.valueOptions == null) {
            String dirName = Globus.getWorkflowConfigDir();
            WORKFLOW_FORM_CONFIG_PROP.valueOptions =
                new DirectoryListConfigurablePropertyValueOptions(
                                                                  dirName,
                                                                  true,
                                                                  SubmissionConfigReader.SUBMIT_DEF_FILE_PREFIX,
                                                                  true, false, true, null);
        }
        return configuration;
    }


    public static String getWorkflowForCollection(Collection collection, Context context)
    {
        return GlobusConfigurationManager.getProperty(context, Globus.CONFIG_SUBMISSION_WORKFLOW,null,
                                                      collection);
    }
}
