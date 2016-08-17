/**
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
 */

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.globus.Globus;
import org.dspace.submit.AbstractProcessingStep;

/**
 * Upload step for DSpace. Processes the actual upload of files
 * for an item being submitted into DSpace.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized
 * by both the JSP-UI and the Manakin XML-UI
 *
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 *
 * @author Tim Donohue
 * @version $Revision$
 */
public class GlobusTransferStep extends AbstractProcessingStep
{
    /** Button to upload a file * */
    public static final String SUBMIT_UPLOAD_BUTTON = "submit_upload";

    /** Button to skip uploading a file * */
    public static final String SUBMIT_SKIP_BUTTON = "submit_skip";

    /** Button to submit more files * */
    public static final String SUBMIT_MORE_BUTTON = "submit_more";

    /** Button to cancel editing of file info * */
    public static final String CANCEL_EDIT_BUTTON = "submit_edit_cancel";

    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     *
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // integrity error occurred
    public static final int STATUS_INTEGRITY_ERROR = 1;

    // error in uploading file
    public static final int STATUS_UPLOAD_ERROR = 2;

    // error - no files uploaded!
    public static final int STATUS_NO_FILES_ERROR = 5;

    // format of uploaded file is unknown
    public static final int STATUS_UNKNOWN_FORMAT = 10;

    // virus checker unavailable ?
    public static final int STATUS_VIRUS_CHECKER_UNAVAILABLE = 14;

    // file failed virus check
    public static final int STATUS_CONTAINS_VIRUS = 16;

    // edit file information
    public static final int STATUS_EDIT_BITSTREAM = 20;

    // return from editing file information
    public static final int STATUS_EDIT_COMPLETE = 25;

    /** log4j logger */
    private static Logger log = Logger.getLogger(GlobusTransferStep.class);

    /** is the upload required? */
    protected boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);

    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the appropriate user interface (JSP-UI or XML-UI)
     * <P>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     *
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        log.info("Processing transfer step for subItem: " + subInfo);
	    // get button user pressed
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        // get reference to item
        Item item = subInfo.getSubmissionItem().getItem();

        // if user pressed jump-to button in process bar,
        // return success (so that jump will occur)
        if (buttonPressed.startsWith(PROGRESS_BAR_PREFIX) ||
        		buttonPressed.startsWith(PREVIOUS_BUTTON))
        {
            // check if a file is required to be uploaded
            if (fileRequired && !item.hasUploadedFiles())
            {
                return STATUS_NO_FILES_ERROR;
            }
            else
            {
                return STATUS_COMPLETE;
            }
        }


	// for now only handle new files (if we do this for all cases we will multiply
	// the number of files each time. We need some different behaviour if its an
	// update
	if (buttonPressed.equals(SUBMIT_UPLOAD_BUTTON)){
		int status = processAssociateGlobusEP(context, request, response, subInfo);
             if (status != STATUS_COMPLETE)  {
                return status;
            }
	}

    context.commit();

    return STATUS_COMPLETE;
}

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     *
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     *
     * @return the number of pages in this step
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // Despite using many JSPs, this step only appears
        // ONCE in the Progress Bar, so it's only ONE page
        return 1;
    }



	/*
	 * Process the association of Globus files
	 * This method can be deleted when we fix the workflow.
	 * Used only for testing the workflow.
	 *
	 */
	protected int processAssociateGlobusEP(Context context, HttpServletRequest request,
			HttpServletResponse response, SubmissionInfo subInfo)
					throws ServletException, IOException, SQLException,
					AuthorizeException
					{
		Item item;

		String epName = subInfo.getSubmissionItem().getItem().getGlobusEndpoint();


		try{
			item = subInfo.getSubmissionItem().getItem();
			item.update();
			context.commit();

		}catch (Exception ee){
		    log.error("Exception creating bitstream ", ee);
		}

		return STATUS_COMPLETE;
	}



}
