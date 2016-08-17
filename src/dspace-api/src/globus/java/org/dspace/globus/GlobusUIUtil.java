/*******************************************************************************
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
 *******************************************************************************/


package org.dspace.globus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.transfer.SuccessfulTransfer;
import org.globus.transfer.SuccessfulTransfers;
import org.globus.transfer.Task;
import org.globus.transfer.Task.Status;
import org.globus.transfer.TaskList;

/**
 * A collection of utility functions to help with creating Globus-specific parts
 * of the UI.
 *
 */
public class GlobusUIUtil
{
    private static final Logger logger = Logger.getLogger(GlobusUIUtil.class);

    public static final String TASKLIST_REQUEST_ATTRIBUTE = "globus.tasklist";

    public static final String TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE = "globus.tasksAllDone";

    public static final String TASKS_ANY_FAILED_REQUEST_ATTRIBUTE = "globus.tasksAnyFailed";

    public static final String PRIV_TASKLIST_REQUEST_ATTRIBUTE = "globus.priv.tasklist";

    public static final String PRIV_TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE = "globus.priv.tasksAllDone";

    public static final String PRIV_TASKS_ANY_FAILED_REQUEST_ATTRIBUTE = "globus.priv.tasksAnyFailed";

    private static boolean requestBoolean(HttpServletRequest request,
            String attrName)
    {
        if (request == null || attrName == null)
        {
            return false;
        }
        Object attribute = request.getAttribute(attrName);
        Boolean rVal = Boolean.FALSE;
        if (attribute instanceof Boolean)
        {
            rVal = (Boolean) attribute;
        }
        else if (attribute != null)
        {
            rVal = Boolean.valueOf(attribute.toString());
        }
        return rVal;
    }

    public static boolean allTasksComplete(HttpServletRequest request,
            boolean userTasks)
    {
        if (userTasks)
        {
            return requestBoolean(request,
                    TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE);
        }
        else
        {
            return requestBoolean(request,
                    PRIV_TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE);
        }
    }

    public static boolean anyTaskFailures(HttpServletRequest request,
            boolean userTasks)
    {
        if (userTasks)
        {
            return requestBoolean(request, TASKS_ANY_FAILED_REQUEST_ATTRIBUTE);
        }
        else
        {
            return requestBoolean(request,
                    PRIV_TASKS_ANY_FAILED_REQUEST_ATTRIBUTE);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Task> getTasksForDestEndpoint(HttpServletRequest request,
            GlobusClient client, String epName, String sharePath,
            boolean userTasks)
    {
        List<Task> reqTaskList = null;
        if (request != null) {
            if (userTasks) {
                reqTaskList = (List<Task>) request.getAttribute(TASKLIST_REQUEST_ATTRIBUTE);
            } else {
                reqTaskList = (List<Task>) request.getAttribute(PRIV_TASKLIST_REQUEST_ATTRIBUTE);
            }
        }
        if (reqTaskList != null) {
            return reqTaskList;
        }
        
        if (epName == null)
        {
            logger.warn(
                    "getTasksForDestEndpoint called with null epName, just returning null");
            return null;
        }
        // Set up the return fields we want from this...
        Task resultFieldsTask = new Task();
        resultFieldsTask.destinationEndpoint = "";
        resultFieldsTask.destinationEndpointId = "";
        resultFieldsTask.requestTime = new Date();
        resultFieldsTask.completionTime = new Date();
        resultFieldsTask.status = Status.ACTIVE;
        resultFieldsTask.bytesTransferred = 0L;
        resultFieldsTask.taskId = "";
        resultFieldsTask.sourceEndpoint = "";

        Task filterVals = null;
        // for globuspublish transfers we can check the label
        if (!userTasks)
        {
            filterVals = new Task();
            filterVals.label = Globus.escapeTransferLabel(epName + sharePath);
        }

        // 1000 is kinda a kludge, we need to get all the transfers for this guy
        // so we can find
        // the ones we want. Not so sure about the security issues of getting
        // all of a user's
        // transfer history either.
        TaskList taskList = null;
        try
        {
            taskList = client.taskList(filterVals, resultFieldsTask, 0, 1000);
        }
        catch (GlobusClientException e)
        {
            logger.warn("Failed to get tasks for endpoint " + epName, e);
        }

        List<Task> allTasks = new ArrayList<Task>();
        boolean allDone = true;
        boolean someFailed = false;
        if (taskList != null && taskList.data != null)
        {
            for (Task task : taskList.data)
            {
                if ((epName.equalsIgnoreCase(task.destinationEndpoint) || epName.equals(task.destinationEndpointId))
                        && (!userTasks || doesTaskPathMatchEndpointPath(client,
                                task, sharePath)))
                {
                    allTasks.add(task);
                    switch (task.status)
                    {
                    case ACTIVE:
                    case INACTIVE:
                        allDone = false;
                        break;
                    case FAILED:
                        allDone = false;
                        someFailed = true;
                        break;
                    case SUCCEEDED:
                        // No change to the status flags
                        break;
                    }

                }
            }
        }
        if (request != null)
        {
            if (userTasks)
            {
                request.setAttribute(TASKLIST_REQUEST_ATTRIBUTE, allTasks);
                request.setAttribute(TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE,
                        allDone);
                request.setAttribute(TASKS_ANY_FAILED_REQUEST_ATTRIBUTE,
                        someFailed);
            }
            else
            {
                request.setAttribute(PRIV_TASKLIST_REQUEST_ATTRIBUTE, allTasks);
                request.setAttribute(PRIV_TASKS_ALL_COMPLETE_REQUEST_ATTRIBUTE,
                        allDone);
                request.setAttribute(PRIV_TASKS_ANY_FAILED_REQUEST_ATTRIBUTE,
                        someFailed);
            }
        }
        return allTasks;
    }

    // check if the subtask within a task matches the right shared endpoint path
    // (for a dataset)
    public static boolean doesTaskPathMatchEndpointPath(GlobusClient client,
            Task task, String sharePath)
    {
        switch (task.status)
        {
        case ACTIVE:
        case INACTIVE:
            // we have no way to know if these tasks are going to the path or
            // just the EP
            // for now we'll block on any transfers to the EP so return true
            return true;
        case FAILED:
            // TODO something?
            break;
        case SUCCEEDED:
            SuccessfulTransfers successfulTransfers = null;
            try
            {
                // for now we assume all subtasks are going to a single
                // destination
                successfulTransfers = client.successfulTransfers(task.taskId,
                        null, 1);
            }
            catch (GlobusClientException e)
            {
                // Seems we should ignore this silently as it is not uncommon to not get history
                // for a particular task
            }
            if (successfulTransfers != null && successfulTransfers.data != null)
            {
                for (SuccessfulTransfer transfer : successfulTransfers.data)
                {
                    return transfer.destPath.startsWith(sharePath);
                }
            }
            break;
        }

        return false;
    }

    public static String formatCollectionForDisplay(Collection coll)
    {
        return formatCollectionForDisplay(coll, -1);
    }

    public static String formatCollectionForDisplay(Collection coll, int maxDepth)
    {
        String fullName;
        String collName = coll.getName();
        fullName = collName;
        int curDepth = 0;
        try
        {
            HashSet<Community> seenCommunities = new HashSet<Community>();
            Community[] communities = coll.getCommunities();
            for (Community community : communities)
            {
                if (seenCommunities.contains(community))
                {
                    continue;
                }
                fullName = fullName + " « " + community.getName();
                curDepth++;
                if (maxDepth >= 0 && curDepth >= maxDepth) {
                    break;
                }
                seenCommunities.add(community);
                Community parentCommunity;
                do
                {
                    parentCommunity = community.getParentCommunity();
                    if (parentCommunity != null
                            && !seenCommunities.contains(parentCommunity))
                    {
                        fullName = fullName + " « "
                                + parentCommunity.getName();
                        seenCommunities.add(parentCommunity);
                        community = parentCommunity;
                    }
                }
                while (parentCommunity != null);
            }
        }
        catch (SQLException e)
        {
            logger.warn("Failed to get all communities for collection " + coll, e);
        }
        return fullName;
    }
}
