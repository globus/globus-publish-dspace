/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */


package org.dspace.app.webui.jsptag;

import static org.dspace.globus.GlobusHtmlUtil.div;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.globus.Globus;
import org.dspace.globus.configuration.BaseConfigurable;
import org.dspace.globus.configuration.BasicConfigurablePropertyValueOptions;
import org.dspace.globus.configuration.BootstrapFormRenderer;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.Configurable.ConfigurableProperty;
import org.dspace.globus.configuration.Configurable.DataType;
import org.dspace.globus.configuration.DirectoryListConfigurablePropertyValueOptions;

/**
 *
 */
public class SubmissionConfigTag extends TagSupport
{
    private static final Logger logger = Logger.getLogger(SubmissionConfigTag.class);

    private static final long serialVersionUID = 1L;

    private DSpaceObject dso;
    private Context context;

    /** Strings stored for different types of curation. Index in to this array should match
     * the worfklow step number they correspond to.
     */
    public static String[] CURATION_TYPES = { "None", "Accept/Reject", "Edit Metadata"};

    public static final String ACCEPT_REJECT_CURATION = "globus.collection.curation.accept_reject";

    public static final String EDIT_CURATION = "globus.collection.curation.edit";



    public void setDSpaceObject(DSpaceObject dso)
    {
        this.dso = dso;
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
            String rendering = (String) bfr.renderFrom(getConfiguration(dso), context, dso);
            if (rendering != null) {
                panelFields.append(rendering);
            }

            writer.append(div("class=panel panel-default",
                              div("class=panel-heading", "Workflow Settings"),
                              div("class=panel-body", panelFields.toString())));
            writer.append("\n");

        } catch (IOException e) {
            throw new JspException("Failed created the form configuration panel", e);
        }
        return SKIP_BODY;
    }


    /**
     * Doing this in a non-static way so that we can read the configuration needed for setting the
     * location of the forms directory. Synchronized so we can just init it one time.
     * @return
     */
    public static synchronized Configurable getConfiguration(DSpaceObject dso)
    {
        Set<String> dsoIds = new HashSet<String>();
        List<Community> communities = null;
        try {
            if (dso instanceof Collection) {
                Collection collection = (Collection) dso;
                int collectionId = dso.getID();
                dsoIds.add("collection_" + collectionId);
                communities = Arrays.asList(collection.getCommunities());
            } else if (dso instanceof Community) {
                Community community = (Community) dso;
                communities = new ArrayList<Community>();
                communities.add(community);
                communities.addAll(Arrays.asList(community.getAllParents()));
            }
            if (communities != null) {
                for (Community community : communities) {
                    dsoIds.add("community_" + community.getID());
                }
            }
        } catch (SQLException e) {
            logger.warn("Error getting communties for object: " + dso.getName(), e);
        }

        ConfigurableProperty formFileConfigProp =
            new ConfigurableProperty(Globus.COLLECTION_INPUT_FORM_CFG_PROP, "Input Form",
                                     "input-forms-datacite-mandatory.xml", DataType.STRING, "",
                                     true);
        String dirName = Globus.getFormConfigDir();
        formFileConfigProp.valueOptions =
            new DirectoryListConfigurablePropertyValueOptions(dirName, true, "input-forms",
                                                              true, true, true, dsoIds);

        ConfigurableProperty workflowFormConfigProp =
            new ConfigurableProperty(Globus.CONFIG_SUBMISSION_WORKFLOW, "Submission Workflow",
                                     "item-submission.xml", DataType.STRING, "", true);
        dirName = Globus.getWorkflowConfigDir();
        workflowFormConfigProp.valueOptions =
            new DirectoryListConfigurablePropertyValueOptions(
                                                              dirName,
                                                              true,
                                                              SubmissionConfigReader.SUBMIT_DEF_FILE_PREFIX,
                                                              true, true, true, dsoIds);

        ConfigurableProperty curationTypeConfigProp =
            new ConfigurableProperty(Globus.CONFIG_CURATION_TYPE, "Curation Type",
                                     CURATION_TYPES[1] /* Accept/reject */, DataType.STRING, "",
                                     true);
        curationTypeConfigProp.valueOptions =
                        new BasicConfigurablePropertyValueOptions(CURATION_TYPES);

        Configurable configuration =
                        new BaseConfigurable("Collection Input Form", formFileConfigProp,
                                             workflowFormConfigProp,
                                             curationTypeConfigProp);
        return configuration;
    }
}
