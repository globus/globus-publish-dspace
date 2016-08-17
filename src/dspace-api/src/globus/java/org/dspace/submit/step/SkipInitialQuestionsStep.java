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
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.globus.GlobusStorageManagement;
import org.dspace.submit.AbstractProcessingStep;

/**
 * This is a Simple Step class that need to be used when you want skip the
 * initial questions step!
 * <p>
 * At the moment this step is required because part of the behaviour of the
 * InitialQuestionStep is required to be managed also in the DescribeStep (see
 * JIRA [DS-83] Hardcoded behaviour of Initial question step in the submission)
 * </p>
 *
 * @see org.dspace.submit.AbstractProcessingStep
 * @see org.dspace.submit.step.InitialQuestionsStep
 * @see org.dspace.submit.step.DescribeStep
 *
 * @author Andrea Bollini
 * @version $Revision$
 */
public class SkipInitialQuestionsStep extends AbstractProcessingStep
{
    /**
     * Simply we flags the submission as the user had checked both multi-title,
     * multi-files and published before so that the input-form configuration
     * will be used as is
     */
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        InProgressSubmission submissionItem = subInfo.getSubmissionItem();
        submissionItem.setMultipleFiles(true);
        submissionItem.setMultipleTitles(true);
        submissionItem.setPublishedBefore(true);
        submissionItem.update();

        // This will always be called for processing when entering the submit workflow whether
        // it is a first time or a resume. So, we make sure the creation of the dataset's directory
        // happens here and that the proper meta-data for it is set.
        Item item = submissionItem.getItem();
        if (item != null)
        {
            Collection[] collections = item.getCollections();
            Collection itemCollection = null;
            if (collections != null && collections.length > 0)
            {
                itemCollection = collections[0];
            }
            else
            {
                itemCollection = submissionItem.getCollection();
            }
            EPerson submitter = submissionItem.getSubmitter();
            GlobusStorageManagement.createUnpublishedDirectoryForItem(context,
                    item, itemCollection, submitter.getGlobusUserName());
        }
        return STATUS_COMPLETE;
    }

    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        return 1;
    }
}
