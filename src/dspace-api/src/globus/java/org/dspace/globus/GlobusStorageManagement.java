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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Collection.PolicyType;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.transfer.TransferAccess;
import org.globus.transfer.TransferAccessList;
import org.globus.transfer.TransferDirectoryListing;
import org.globus.transfer.TransferFile;
import org.globus.transfer.TransferInterface.PrincipalType;

/**
 *
 */
public class GlobusStorageManagement
{
    private static final Logger logger = Logger
            .getLogger(GlobusStorageManagement.class);

    public static String createUnpublishedDirectoryForItem(Context context,
            Item item, Collection collection, String shareUserName)
    {
        logger.info("Creating directory for item.");

        if (item == null || shareUserName == null)
        {
            logger.warn(
                    "createUnpublishedDirectoryForItem called with null item or null shareUserName");
            return null;
        }

        String epName = item.getGlobusEndpoint();
        if (epName == null) {
            epName = Globus.getSharedEndpointName(context, collection);
            item.setGlobusEndpoint(epName);
        }
        String sharePath = item.getGlobusSharePath();
        if (sharePath == null) {
            sharePath = Globus.getUnpublishedPath(context,
                    String.valueOf(item.getID()), collection);
            item.setGlobusSharePath(sharePath);
        }

        // Creation of shared endpoint directory needs to be done by the publish
        // "superuser"
        GlobusClient privClient = Globus.getPrivlegedClient();
        logger.info("Creating shared endpoint directory params " + epName + " "
                + sharePath);

        String dataDirectoryName = Globus
                .getGlobusConfigProperty(Globus.PUBLICATION_DATA_DIRECTORY);

        if (sharePath != null && epName != null)
        {
            logger.info("Creating shared endpoint directory params " + epName
                    + ":" + sharePath);

            // If we create it ok, update the stored ep
            // We want to go on down and create the data directory with the
            // permissions
            // while we are at it.
            if (GlobusStorageManagement.createDirAndSetAcl(privClient, context,
                    collection, epName, sharePath + "/" + dataDirectoryName,
                    shareUserName, null, "rw"))
            {
                try
                {
                    item.update(); // Push the change to be persistent
                }
                catch (Exception e)
                {
                    logger.warn(
                            "Unable to persist update to item's globus endpoint",
                            e);
                }
                return epName + sharePath;
            }
        }

        return null;
    }

    static boolean createDirAndSetAcl(GlobusClient client, Context context,
            Collection collection, String endpointName, String sharePath,
            String shareUserName, Group group, String perms)
    {
        logger.info(
                "Creating shared endpoint: " + endpointName + ":" + sharePath);

        if (client == null)
        {
            logger.warn(
                    "Attempt to execute createSharedEndpoint with no GlobusClient");
            return false;
        }

        if (endpointName == null)
        {
            return false;
        }

        try
        {
            client.activateEndpoint(endpointName);
            // Remove any instances of double (or triple, etc.) / with single /
            sharePath = Globus.fixPath(sharePath, true, true);
            String fullPath = sharePath;
            // Remove any trailing /
            if (fullPath.endsWith("/")) {
                fullPath = fullPath.substring(0, fullPath.length() - 1);
            }

            // The paths that we're missing. We test from longest to shortest, but put in the
            // list in the order shortest to longest
            LinkedList<String> pathsToCreate = new LinkedList<String>();

            int lastSlash = -1;
            while ((lastSlash = fullPath.lastIndexOf('/')) >= 0) {
                String root = fullPath.substring(0, lastSlash);
                String leaf = fullPath.substring(lastSlash + 1);
                if (!checkFileExist(client, endpointName, root, leaf)) {
                    pathsToCreate.addFirst(fullPath);
                } else {
                    break; // Once we've found one of the path members, they all must exist
                }
                fullPath = root;
            }

            for (String toCreate : pathsToCreate)
            {
                if (!client.createDirectory(endpointName, toCreate)) {
                    logger.warn("Unable to create directory "
                            + sharePath);
                    return false;
                }
            }

            changeSharingPermissions(client, endpointName, sharePath,
                    shareUserName,
                    (group != null ? Collections.singletonList(
                            Globus.getUnPrefixedGroupID(group.getName()))
                            : null),
                    perms, perms, false);
        }
        catch (GlobusClientException e)
        {
            logger.error("Unable to establish shared endpoint", e);
            return false;
        }

        return true;
    }

    public static void updateCollectionPubDirectory(Context context,
            Collection collection) throws SQLException, GlobusClientException
    {
        String epName = Globus.getSharedEndpointName(context, collection);
        Group readGroup = collection.getPolicyTargetGroup(PolicyType.READ);
        GlobusClient pc = Globus.getPrivlegedClient();
        String pubDirPath = Globus.getPathToPublishedDir(context, collection);
        pc.activateEndpoint(epName);
        createDirAndSetAcl(pc, context, collection, epName, pubDirPath, null,
                readGroup, "r");
    }

    /**
     * @param gc
     * @param epName
     * @param rootPath
     * @param fileName
     * @param dirExists
     * @return
     * @throws GlobusClientException
     */
    private static boolean checkFileExist(GlobusClient gc, String epName,
            String rootPath, String fileName)
    {

        TransferDirectoryListing listing = null;
        try
        {
            listing = gc.lsDirectory(epName, rootPath);
        }
        catch (GlobusClientException e)
        {
            // We interpret a failure to ls as something in the path doesn't exist, so return false
            return false;
        }
        if (listing != null)
        {
            TransferFile[] files = listing.files;
            if (files != null)
            {
                for (TransferFile file : files)
                {
                    if (fileName.equals(file.name))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Remove all permissions (ACLs) from a path or a sub-tree.
     *
     * @param client
     *            The client to be used when performing the operations.
     * @param epName
     *            The name of the endpoint on which the path resides.
     * @param sharePath
     *            The path to the directory or root of the hierarchy to remove
     * @param sharePathIsPrefix
     *            if {@code true} then treat the sharePath as a root and remove
     *            any permissions rooted here. If {@code false} only remove
     *            permissions on this specific directory path.
     * @return {@code true} if successful, {@code false} otherwise
     */
    public static boolean removePermissions(GlobusClient client, String epName,
            String sharePath, boolean sharePathIsPrefix)
    {
        logger.info("Removing permissions " + epName + " " + sharePath);
        TransferAccessList accessList = null;

        try
        {
            accessList = client.getAccessList(epName);
        }
        catch (GlobusClientException gce)
        {
            logger.error("Unable to get access list for endpoint " + epName,
                    gce);
            return false;
        }
        TransferAccess[] items = accessList.accessItems;
        boolean allGood = true;
        String globusPrivUserName = ConfigurationManager.getProperty(
                Globus.GLOBUS_AUTH_CONFIG_MODULE,
                Globus.GLOBUS_USER_CONFIG_PROP);

        if (items != null)
        {
            for (TransferAccess accessItem : items)
            {
                // Remove any permissions that relate to this path. Only leave
                // the owning user.
                boolean pathsMatch = false;
                if (sharePathIsPrefix) {
                    pathsMatch = accessItem.path.startsWith(sharePath);
                } else {
                    pathsMatch = sharePath.equals(accessItem.path);
                }
                if (globusPrivUserName != null
                        && pathsMatch
                        && !globusPrivUserName.equals(accessItem.principal))
                {
                    try
                    {
                        client.deleteAccess(epName, accessItem);
                    }
                    catch (GlobusClientException e)
                    {
                        logger.error("Failed to remove permission id "
                                + accessItem.id + " from endpoint " + epName
                                + sharePath, e);
                        allGood = false;
                    }
                }
            }
        }
        return allGood;
    }

    public static boolean changeArchiveSharingPermissions(GlobusClient client,
            Context c, Item item)
    {
        // find what groups are allowed to read the item..
        Group[] groups = null;
        Collection collection = null;
        try
        {
            collection = item.getOwningCollection();
            if (collection != null)
            {
                groups = new Group[] { collection
                        .getPolicyTargetGroup(Collection.PolicyType.READ) };
            }
        }
        catch (Exception e1)
        {
            logger.warn("Cannot get owning group for item " + item.getHandle());
        }
        try
        {
            // Set the groups based on the owning collection's permissions
            if (groups == null)
            {
                groups = AuthorizeManager.getAuthorizedGroups(c, item,
                        Constants.READ);
            }
        }
        catch (SQLException e)
        {
            logger.error("Cant get authorized groups for item");
            return false;
        }

        String globusEndpoint = item.getGlobusEndpoint();
        String sharePath = item.getGlobusSharePath();

        if (globusEndpoint == null || sharePath == null)
        {
            logger.warn(
                    "Unable to get name or path of globus endpoint for item "
                            + item);
            return false;
        }

        String submitter = null;
        try
        {
            submitter = item.getSubmitter().getGlobusUserName();
        }
        catch (SQLException e)
        {
            logger.error("Exception getting item submitter");
            return false;
        }

        ArrayList<String> shareGroups = new ArrayList<String>();
        boolean publicSharing = false;
        if (groups != null)
        {
            for (Group g : groups)
            {
                if (g.isAnon())
                {
                    publicSharing = true;
                }
                if (Globus.isGlobusGroup(g.getName()))
                {
                    shareGroups.add(Globus.getUnPrefixedGroupID(g.getName()));
                }
                for (Group childGroup : g.getMemberGroups())
                {
                    if (Globus.isGlobusGroup(childGroup.getName()))
                    {
                        shareGroups.add(Globus
                                .getUnPrefixedGroupID(childGroup.getName()));
                    }
                }
            }
        }
        return changeSharingPermissions(client, globusEndpoint, sharePath,
                submitter, shareGroups, "r", "r", publicSharing);
    }

    public static boolean changeUserGroupSharingPermissions(GlobusClient client,
            Item item, Group group)
    {
        String globusEndpoint = item.getGlobusEndpoint();
        String sharePath = item.getGlobusSharePath();

        if (globusEndpoint == null || sharePath == null)
        {
            logger.warn(
                    "Unable to get name or path of globus endpoint for item "
                            + item);
            return false;
        }

        String submitter = null;
        try
        {
            submitter = item.getSubmitter().getGlobusUserName();
        }
        catch (SQLException e)
        {
            logger.error("Exception getting item submitter");
            return false;
        }

        if (group == null)
        {
            return changeSharingPermissions(client, globusEndpoint, sharePath,
                    submitter, null, "r", null, false);
        }

        ArrayList<String> shareGroups = new ArrayList<String>();
        for (Group childGroup : group.getMemberGroups())
        {
            if (Globus.isGlobusGroup(childGroup.getName()))
            {
                shareGroups
                        .add(Globus.getUnPrefixedGroupID(childGroup.getName()));
            }
        }
        return changeSharingPermissions(client, globusEndpoint, sharePath,
                submitter, shareGroups, "r", "r", false);

    }

    public static boolean changeSharingPermissions(GlobusClient client,
            String epName, String sharePath, String username,
            List<String> shareGroups, String userPerms, String groupPerms,
            boolean publicSharing)
    {
        if (!removePermissions(client, epName, sharePath, true))
        {
            logger.warn("Cound not remove current permissions for endpoint "
                    + epName);
            return false;
        }

        boolean success = true;
        if (username != null)
        {
            try
            {
                client.setEndpointAccessPermissions(epName, sharePath,
                        PrincipalType.identity, username, userPerms, true);
            }
            catch (GlobusClientException e)
            {
                logger.warn("Failed to set permission for identity " + username
                        + " on endpoint " + epName, e);
                success = false;
            }
        }

        if (shareGroups != null)
        {
            for (String shareGroup : shareGroups)
            {
                // We record setting things public using the Anonymous
                // group in many cases, so
                // check for that here and set the flag even if it wasn't passed
                // in.
                if (Group.ANONYMOUS_GROUP_NAME.equals(shareGroup))
                {
                    publicSharing = true;
                    continue;
                }
                try
                {
                    client.setEndpointAccessPermissions(epName, sharePath,
                            PrincipalType.group, shareGroup, groupPerms, true);
                }
                catch (GlobusClientException e)
                {
                    logger.warn("Failed to set permission for group "
                            + shareGroup + " on endpoint " + epName, e);
                    success = false;
                }
            }
        }

        if (publicSharing)
        {
            try
            {
                client.setEndpointAccessPermissions(epName, sharePath,
                        PrincipalType.all_authenticated_users, "", groupPerms,
                        true);
            }
            catch (GlobusClientException e)
            {
                logger.warn("Failed to set permission for public on endpoint "
                        + epName, e);
                success = false;
            }

        }
        return success;
    }

    /**
     * Begin transfer of the artifact file to the dataset endpoint returning the
     * taskId for the transfer job so that it can be monitored later.
     *
     * @param context
     * @param globusClient
     * @param item
     * @param collection
     * @return The taskId to be monitored or {@code null} if an error occurs or
     *         an Empty String if ("") if the collection is set to not create an
     *         artifcat file.
     */
    public static String createMetadataFile(Context context,
            GlobusClient globusClient, Item item, Collection collection)
    {
        logger.info("Creating metadata file for item " + item.getID());
        try {
            // when setting artifact during the submission process we don't yet
            // have the collection
            // associated with the item
            if (collection == null)
            {
                collection = item.getOwningCollection();
            }
        }
        catch (Exception e) {
            logger.info("Can't get owning collection for item.", e);
            return null;
        }

        // Get all item metadata
        String metadata = Globus.getAllMetadata(context, item, collection);
        String stagingMethod = Globus.getGlobusConfigProperty(Globus.STAGING_METHOD);
        if (stagingMethod == null) { stagingMethod = "s3"; }
        
        if (stagingMethod.equalsIgnoreCase("local") ) {
        	return createAndTransferLocal(globusClient,item, metadata);
        } else {
        	return createAndTransferS3(globusClient, item, metadata);
        }
    }

    /**
     * @param globusClient
     * @param item
     * @param metadata
     */
    public static String createAndTransferLocal(GlobusClient globusClient,Item item, String metadata)
    {
    	//
        // Local filesystem for staging
    	//
    	String localStagingPath = Globus.getGlobusConfigProperty(Globus.STAGING_LOCAL_PATH);
    	if (localStagingPath == null || localStagingPath.equalsIgnoreCase("")) {
    		logger.error("staging.method is local but staging.local_path not defined in globus.cfg");
    		return null;
    	}
        if (!localStagingPath.endsWith("/")) {
        	localStagingPath = localStagingPath + "/";
        }
    	if (!Files.exists(Paths.get(localStagingPath))) {
    	    try {
    	    	Files.createDirectories(Paths.get(localStagingPath));
    	    } 
    	    catch(IOException e){
        		logger.error("Could not create local staging dir " + localStagingPath + " - check globus.cfg");
        		return null;
    	    }
    	}
        if (!Files.isDirectory(Paths.get(localStagingPath))) {        	
    		logger.error(localStagingPath + "is not a directory! See local_staging_path in globus.cfg");
    		return null;
    	}
    	if (!Files.isWritable(Paths.get(localStagingPath))) {	
    		logger.error(localStagingPath + "is not writable, please fix permissions");
    		return null;
    	}
    	String objectID = UUID.randomUUID().toString();
    	try {
    		Files.write(Paths.get(localStagingPath + objectID), metadata.getBytes("UTF-8"), StandardOpenOption.CREATE);
    		logger.info("Wrote metadata to local file:" + localStagingPath + objectID);
    	}
    	catch (Exception e) {
    		logger.error("Error saving metadata to local filesystem: " + e);
    	}

        // transfer the file
        try {
            String srcEndpoint = Globus.getGlobusConfigProperty(Globus.STAGING_LOCAL_ENDPOINT);
            String srcPath = Globus.getGlobusConfigProperty(Globus.STAGING_LOCAL_ENDPOINT_PATH);
            if (!srcPath.endsWith("/")) {
            	srcPath = srcPath + "/";
            }
            srcPath = srcPath + objectID;
            String destPath = item.getGlobusSharePath() + "/" + Globus.getGlobusConfigProperty(Globus.ARTIFACT_FILE_NAME);
            // We cannot delete the artifact file here because this transfer is asynchronous
            return Globus.transferFiles(globusClient, srcEndpoint, srcPath,
                    item.getGlobusEndpoint(), destPath,
                    Globus.escapeTransferLabel(item.getGlobusEndpoint()
                            + item.getGlobusSharePath()));
        }
        catch (Exception e) {
            logger.error("Error transferring metadata file from local: " + e);
    		return null;
        }
    }
    
    /**
     * @param globusClient
     * @param item
     * @param metadata
     */
    public static String createAndTransferS3(GlobusClient globusClient, Item item, String metadata)
    {
    	//
    	// Amazon S3 for staging
    	//
        if (Globus.s3Client == null) {
            if (Globus.getGlobusConfigBooleanProperty(Globus.S3_INSTANCE_GROUP_AUTH)) {
                logger.info("Creating S3 client using Instance Group");
                Globus.s3Client = new S3Client(
                        Globus.getGlobusConfigProperty(Globus.S3_BUCKET), null,
                        null);
            } else
            {
                logger.info("Creating S3 client using AWS Creds");
                Globus.s3Client = new S3Client(
                        Globus.getGlobusConfigProperty(Globus.S3_BUCKET),
                        Globus.getGlobusConfigProperty(Globus.AWS_ACCESS_KEY_ID),
                        Globus.getGlobusConfigProperty(Globus.AWS_SECRET_ACCESS_KEY));
            }
        }
        String objectID = Globus.s3Client.createObject(metadata);
        
        // transfer the file
        try {
            String srcEndpoint = Globus.getGlobusConfigProperty(Globus.S3_ENDPOINT);
            String srcPath = "/" + objectID;
            String destPath = item.getGlobusSharePath() + "/"
                    + Globus.getGlobusConfigProperty(Globus.ARTIFACT_FILE_NAME);
            // We cannot delete the artifact file here because this transfer is asynchronous
            return Globus.transferFiles(globusClient, srcEndpoint, srcPath,
                    item.getGlobusEndpoint(), destPath,
                    Globus.escapeTransferLabel(item.getGlobusEndpoint()
                            + item.getGlobusSharePath()));
        }
        catch (Exception e) {
            logger.error("Error transferring metadata file from s3: " + e);
    		return null;
        }    	
    }
    
    /**
     * @param c
     * @param wi
     * @param newstate
     * @param newowner
     */
    public static void updateStorageForWorkflow(Context c, WorkflowItem wi,
            int newstate, EPerson newowner)
    {
        Item item = wi.getItem();
        Collection collection = wi.getCollection();
        String epName = item.getGlobusEndpoint();
        String dataPath = Globus.getPathToData(item);
        String unPubPath = Globus.getUnpublishedPath(c, String.valueOf(item.getID()), collection);
        if (!unPubPath.endsWith("/")) {
            unPubPath = unPubPath + "/";
        }
        // ACL setting requires that path end with /
        if (!dataPath.endsWith("/"))
        {
            dataPath = dataPath + "/";
        }
        GlobusClient globusClient = Globus.getPrivlegedClient();
        String newOwnerName = null;
        if (newowner != null)
        {
            newOwnerName = newowner.getGlobusUserName();
        }
        switch (newstate)
        {
        case WorkflowManager.WFSTATE_STEP1POOL:
        case WorkflowManager.WFSTATE_STEP2POOL:
        case WorkflowManager.WFSTATE_STEP3POOL:
            // When we get to any of the pool states, there's no active process
            // going on, so
            // we move the data directory back to a closed state
            removePermissions(globusClient, epName, unPubPath, true);
            break;

        case WorkflowManager.WFSTATE_STEP1:
            // Read only curation starting
            changeSharingPermissions(globusClient, epName, unPubPath,
                    newOwnerName, null, "r", null, false);
            break;

        case WorkflowManager.WFSTATE_STEP2:
            // Update curation starting
            changeSharingPermissions(globusClient, epName, unPubPath,
                    newOwnerName, null, "rw", null, false);
            break;

        case WorkflowManager.WFSTATE_SUBMIT:
            // Starting the submit workflow and we'll give the user rw
            // access as well.

            changeSharingPermissions(globusClient, epName, dataPath,
                    newOwnerName, null, "rw", null, false);
            break;

        case WorkflowManager.WFSTATE_STEP3:
            // This form of curation not supported
            break;

        case WorkflowManager.WFSTATE_ARCHIVE:
            try
            {
                updateCollectionPubDirectory(c, collection);
                removePermissions(globusClient, epName, unPubPath, true);

                String publishedPath = Globus.getPublishedPath(c,
                        String.valueOf(item.getID()), collection);
                String currentDir = item.getGlobusSharePath();
                    if (globusClient.rename(epName, currentDir, publishedPath))
                    {
                        // Note, inside try catch block so we don't do this if the
                        // rename throws exception
                        item.setGlobusSharePath(publishedPath);
                    }
            }
            catch (SQLException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (GlobusClientException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            break;
        }
    }
}
