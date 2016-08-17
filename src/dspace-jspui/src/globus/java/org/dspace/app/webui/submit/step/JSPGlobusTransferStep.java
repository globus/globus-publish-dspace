/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

package org.dspace.app.webui.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.sherpa.submit.SHERPASubmitService;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.servlet.SubmissionController;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.globus.Globus;
import org.dspace.globus.GlobusUIUtil;
import org.dspace.submit.step.UploadStep;
import org.dspace.utils.DSpace;
import org.globus.transfer.Task;

/**
 * Transfer step for Globus Publish in the DSpace JSP-UI. Sets up transfers using the Globus web
 * site and provides monitoring of the transfer status.
 * @see org.dspace.app.webui.servlet.SubmissionController
 * @see org.dspace.app.webui.submit.JSPStep
 * @see org.dspace.submit.step.UploadStep
 *
 */
public class JSPGlobusTransferStep extends JSPStep
{

    private static final Logger logger = Logger.getLogger(JSPGlobusTransferStep.class);

    /** JSP to choose files to upload * */
    private static final String CHOOSE_FILE_JSP = "/submit/globus-choose-file.jsp";

    /** JSP to show files that were uploaded * */
    private static final String UPLOAD_LIST_JSP = "/submit/globus-upload-file-list.jsp";

    /** JSP to review uploaded files * */
    private static final String REVIEW_JSP = "/submit/globus-review-upload.jsp";

    /**
     * Do any pre-processing to determine which JSP (if any) is used to generate the UI for this
     * step. This method should include the gathering and validating of all data required by the
     * JSP. In addition, if the JSP requires any variable to passed to it on the Request, this
     * method should set those variables.
     * <P>
     * If this step requires user interaction, then this method must call the JSP to display, using
     * the "showJSP()" method of the JSPStepManager class.
     * <P>
     * If this step doesn't require user interaction OR you are solely using Manakin for your user
     * interface, then this method may be left EMPTY, since all step processing should occur in the
     * doProcessing() method.
     *
     * @param context current DSpace context
     * @param request current servlet request object
     * @param response current servlet response object
     * @param subInfo submission info object
     */
    @Override
    public void doPreProcessing(Context context, HttpServletRequest request,
                                HttpServletResponse response, SubmissionInfo subInfo)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // pass on the fileupload setting
        if (subInfo != null) {
            InProgressSubmission subItem = subInfo.getSubmissionItem();
            Item item = subItem.getItem();
            Collection c = subItem.getCollection();
            try {
                DCInputsReader inputsReader = new DCInputsReader();
                request.setAttribute("submission.inputs", inputsReader.getInputs(c.getHandle()));
            } catch (DCInputsReaderException e) {
                throw new ServletException(e);
            }

            prepTransferPage(context, request, item, c);
            // show the transfer page
            showTransferPage(context, request, response, subInfo, false);
        } else {
            throw new IllegalStateException("SubInfo must not be null");
        }
    }


    /**
     * @param context
     * @param request
     * @param item
     * @param c
     */
    private void prepTransferPage(Context context, HttpServletRequest request, Item item,
                                  Collection c)
    {

        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            logger.warn("Cannot get user for request " + request);
            return;
        }

        //String epName = Globus.createSharedEndpointForItem(item, c, currentUser.getNetid());
        String epName = item.getGlobusEndpoint();
        // Only have rights to the data directory, so we need to drill down there when
        // checking for transfers.
        String sharePath = Globus.getPathToData(item);

        // We probably could detect if this was newly created and not bother checking for outstanding
        // tasks to the ep, but seems slightly risky in terms of async. operations at the endpoint
        if (epName != null) {
            Globus g = Globus.getGlobusClientFromContext(context);
            if (g == null) {
                logger.warn("No globus client available for context: ");
                return;
            }
            List<Task> tasks = GlobusUIUtil.getTasksForDestEndpoint(request, g.getClient(), epName,
                                                                    sharePath, true);
        }
    }


    /**
     * Do any post-processing after the step's backend processing occurred (in the doProcessing()
     * method).
     * <P>
     * It is this method's job to determine whether processing completed successfully, or display
     * another JSP informing the users of any potential problems/errors.
     * <P>
     * If this step doesn't require user interaction OR you are solely using Manakin for your user
     * interface, then this method may be left EMPTY, since all step processing should occur in the
     * doProcessing() method.
     *
     * @param context current DSpace context
     * @param request current servlet request object
     * @param response current servlet response object
     * @param subInfo submission info object
     * @param status any status/errors reported by doProcessing() method
     */
    @Override
    public void doPostProcessing(Context context, HttpServletRequest request,
                                 HttpServletResponse response, SubmissionInfo subInfo, int status)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String buttonPressed = UIUtil.getSubmitButton(request, UploadStep.NEXT_BUTTON);
        // Do we need to skip the upload entirely?
       boolean fileRequired =
            ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);

        return;
    }


    /**
     * Display the appropriate upload page in the file upload sequence. Which page this is depends
     * on whether the user has uploaded any files in this item already.
     *
     * @param context the DSpace context object
     * @param request the request object
     * @param response the response object
     * @param subInfo the SubmissionInfo object
     * @param justUploaded true, if the user just finished uploading a file
     */
    protected void showTransferPage(Context context, HttpServletRequest request,
                                  HttpServletResponse response, SubmissionInfo subInfo,
                                  boolean justUploaded) throws SQLException, ServletException,
        IOException
    {
        showChooseFile(context, request, response, subInfo);

        /*
        // if user already has uploaded at least one file
        if (justUploaded || fileAlreadyUploaded) {
            // The item already has files associated with it.
            showUploadFileList(context, request, response, subInfo, justUploaded, false);
        } else {
            // show the page to choose a file to upload
            showChooseFile(context, request, response, subInfo);
        }
        */
    }


    /**
     * Show the page which allows the user to choose another file to upload
     *
     * @param context current DSpace context
     * @param request the request object
     * @param response the response object
     * @param subInfo the SubmissionInfo object
     */
    protected void showChooseFile(Context context, HttpServletRequest request,
                                  HttpServletResponse response, SubmissionInfo subInfo)
        throws SQLException, ServletException, IOException
    {
        if (ConfigurationManager.getBooleanProperty("webui.submission.sherparomeo-policy-enabled",
                                                    true)) {
            SHERPASubmitService sherpaSubmitService =
                new DSpace().getSingletonService(SHERPASubmitService.class);
            request.setAttribute("sherpa", sherpaSubmitService.hasISSNs(context, subInfo
                .getSubmissionItem().getItem()));
        }

        // set to null the bitstream in subInfo, we need to process a new file
        // we don't need any info about previous files...
        subInfo.setBitstream(null);

        // set a flag whether the current step is UploadWithEmbargoStep
        boolean withEmbargo =
            SubmissionController.getCurrentStepConfig(request, subInfo).getProcessingClassName()
                .equals("org.dspace.submit.step.UploadWithEmbargoStep") ? true : false;
        request.setAttribute("with_embargo", Boolean.valueOf(withEmbargo));

        // load JSP which allows the user to select a file to upload
        JSPStepManager.showJSP(request, response, subInfo, CHOOSE_FILE_JSP);
    }


    /**
     * Show the page which lists all the currently uploaded files
     *
     * @param context current DSpace context
     * @param request the request object
     * @param response the response object
     * @param subInfo the SubmissionInfo object
     * @param justUploaded pass in true if the user just successfully uploaded a file
     * @param showChecksums pass in true if checksums should be displayed
     */
    protected void showUploadFileList(Context context, HttpServletRequest request,
                                      HttpServletResponse response, SubmissionInfo subInfo,
                                      boolean justUploaded, boolean showChecksums)
        throws SQLException, ServletException, IOException
    {
        // Set required attributes
        request.setAttribute("just.uploaded", Boolean.valueOf(justUploaded));
        request.setAttribute("show.checksums", Boolean.valueOf(showChecksums));

        // set a flag whether the current step is UploadWithEmbargoStep
        boolean withEmbargo =
            SubmissionController.getCurrentStepConfig(request, subInfo).getProcessingClassName()
                .equals("org.dspace.submit.step.UploadWithEmbargoStep") ? true : false;
        request.setAttribute("with_embargo", Boolean.valueOf(withEmbargo));

        // Always go to advanced view in workflow mode
        if (subInfo.isInWorkflow() || subInfo.getSubmissionItem().hasMultipleFiles()) {
            // next, load JSP listing multiple files
            JSPStepManager.showJSP(request, response, subInfo, UPLOAD_LIST_JSP);
        } else {
            // next, load JSP listing a single file
//            JSPStepManager.showJSP(request, response, subInfo, UPLOAD_FILE_JSP);
            JSPStepManager.showJSP(request, response, subInfo, "");
        }
    }


    /**
     * Show the page which allows the user to change the format of the file that was just uploaded
     *
     * @param context context object
     * @param request the request object
     * @param response the response object
     * @param subInfo the SubmissionInfo object
     */
    protected void showGetFileFormat(Context context, HttpServletRequest request,
                                     HttpServletResponse response, SubmissionInfo subInfo)
        throws SQLException, ServletException, IOException
    {
        if (subInfo == null || subInfo.getBitstream() == null) {
            // We have an integrity error, since we seem to have lost
            // which bitstream was just uploaded
            logger.warn(LogManager.getHeader(context, "integrity_error",
                                          UIUtil.getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }

        BitstreamFormat[] formats = BitstreamFormat.findNonInternal(context);

        request.setAttribute("bitstream.formats", formats);

        // What does the system think it is?
        BitstreamFormat guess = FormatIdentifier.guessFormat(context, subInfo.getBitstream());

        request.setAttribute("guessed.format", guess);

        // display choose file format JSP next
//        JSPStepManager.showJSP(request, response, subInfo, FILE_FORMAT_JSP);
        JSPStepManager.showJSP(request, response, subInfo, "");
    }


    /**
     * Show the page which allows the user to edit the description of already uploaded files
     *
     * @param context context object
     * @param request the request object
     * @param response the response object
     * @param subInfo the SubmissionInfo object
     */
    protected void showFileDescription(Context context, HttpServletRequest request,
                                       HttpServletResponse response, SubmissionInfo subInfo)
        throws SQLException, ServletException, IOException
    {
        // load JSP which allows the user to select a file to upload
//        JSPStepManager.showJSP(request, response, subInfo, FILE_DESCRIPTION_JSP);
        JSPStepManager.showJSP(request, response, subInfo, "");
    }


    /**
     * Return the URL path (e.g. /submit/review-metadata.jsp) of the JSP which will review the
     * information that was gathered in this Step.
     * <P>
     * This Review JSP is loaded by the 'Verify' Step, in order to dynamically generate a submission
     * verification page consisting of the information gathered in all the enabled submission steps.
     *
     * @param context current DSpace context
     * @param request current servlet request object
     * @param response current servlet response object
     * @param subInfo submission info object
     */
    @Override
    public String getReviewJSP(Context context, HttpServletRequest request,
                               HttpServletResponse response, SubmissionInfo subInfo)
    {
        return REVIEW_JSP;
    }
}
