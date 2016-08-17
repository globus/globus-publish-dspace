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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.globus.configuration.BaseConfigurable;
import org.dspace.globus.configuration.Configurable;
import org.dspace.globus.configuration.Configurable.ConfigurableProperty;
import org.dspace.globus.configuration.Configurable.DataType;
import org.dspace.globus.configuration.GlobusConfigurationManager;
import org.globus.GlobusClient;
import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface.RequestType;
import org.globus.auth.GlobusAuthToken;
import org.globus.auth.GlobusUser;
import org.globus.groups.GlobusGroup;
import org.globus.transfer.Task;
import org.globus.transfer.TaskList;
import org.globus.transfer.TransferItem;
import org.globus.transfer.TransferJob;
import org.json.JSONArray;
import org.json.JSONObject;

public class Globus
{
    private static final Logger logger = Logger.getLogger(Globus.class);

    /**
     *
     */
    private static final String PUBLISH_DASHBOARD_PATH = "/PublishDashboard";

    private static final String GLOBUS_AUTH_TOKEN_SESSION_ATTR_NAME = "globus.globusAuthToken";

    private static final String GLOBUS_GROUP_PREFIX = ":G:";

    private static final String GLOBUS_CONFIG_MODULE = "globus";

    public static final String GLOBUS_AUTH_CONFIG_MODULE = "globus-auth";

    // Globus web URLS
    private static final String GLOBUS_URL_PROP = "globus.url";

    private static final String GLOBUS_AUTH_URL_PROP = "globus.auth.url";

    private static final String GLOBUS_TRANSFER_URL_CONFIG_PROP = "transfer.url";

    private static final String GLOBUS_GROUPS_URL_CONFIG_PROP = "groups.url";

    private static final String LOGOUT_URL_CONFIG_PROP = "globus.logout.url";

    // Globus APIs
    private static final String GLOBUS_TRANSFER_API_PROP = "transfer.api";

    private static final String GLOBUS_GROUP_API_PROP = "group.api";

    private static final String GLOBUS_IDENTITY_API_PROP = "identity.api";

    static final String GLOBUS_USER_CONFIG_PROP = "username";

    private static final String GLOBUS_USER_PWD_CONFIG_PROP = "password";

    private static final String GLOBUS_USER_TOKEN_CONFIG_PROP = "token";

    private static final String GLOBUS_GOAUTH_CLIENTID_CONFIG_PROP = "goauth.clientId";

    public static final String PUBLICATION_DATA_DIRECTORY = "publication.data.directory";

    public static final String ARTIFACT_FILE_NAME = "artifact.file.name";

    // Temporary storage for item metadata files
    public static final String STAGING_METHOD = "staging.method";

    public static final String STAGING_LOCAL_PATH = "staging.local_path";

    public static final String STAGING_LOCAL_ENDPOINT = "staging.local_endpoint";

    public static final String STAGING_LOCAL_ENDPOINT_PATH = "staging.local_endpoint_path";

    public static final String S3_BUCKET = "s3.bucket";

    public static final String S3_ENDPOINT = "s3.endpoint";

    public static final String S3_INSTANCE_GROUP_AUTH = "aws.instance.group.auth";

    // this value is not required if using security groups/policies for access
    public static final String AWS_ACCESS_KEY_ID = "aws.access.key.id";

    // this value is not required if using security groups/policies for access
    public static final String AWS_SECRET_ACCESS_KEY = "aws.secret.access.key";

    private static final String GLOBUS_SERVER_ID_PROP = "globus.server-id";

    private static final String TRANSFER_WAIT_TIME_PROP = "globus.transfer.wait.time";

    // Keys for accessing Collection Globus metadata
    public static final String CONFIG_GLOBUS_ENDPOINT_NAME = "globus.endpoint.name";

    public static final String CONFIG_GLOBUS_SHARE_PATH_PREFIX = "globus.share.path.prefix";

    // Config properties for storage of input forms and workflows
    public static final String GLOBUS_FORMS_DIR_CONFIG_PROP = "globus.forms.dir";

    public static final String GLOBUS_WORKFLOW_DIR_CONFIG_PROP = "globus.workflows.dir";

    // Config properties for directories where data will be stored
    public static final String GLOBUS_INPROGRESS_DIR_PROP = "globus.inprogress.directory";

    public static final String GLOBUS_COMPLETED_DIR_PROP = "globus.completed.directory";

    public static final String GLOBUS_PUBLICATION_DIR_PREFIX = "globus.publication.dir.prefix";

    // Banner to display on the collection configuration page
    private static final String GLOBUS_CONFIG_COLLECTION_BANNER_MSG = "globus.collection.banner.msg";

    public static final String GLOBUS_METADATA_MAPPING_PROP = "globus.metadata.mapping";

    private static final String DEFAULT_CONFIG_GLOBUS_SHARE_PATH_PREFIX = "globus.default.sharepath";

    // config key for submission workflow collection property
    public static final String CONFIG_SUBMISSION_WORKFLOW = "globus.collection.submission-workflow";

    // config key for curation type collection property
    public static final String CONFIG_CURATION_TYPE = "globus.curation.curation.type";

    private GlobusClient client;

    public static final String COLLECTION_INPUT_FORM_CFG_PROP = "globus.collection.input-form";

    /**
     * This is a client with increased rights above and beyond the user of
     * DSpace/Globus. 
     */
    private static GlobusClient privClient;
    /**
     * Estimated time when the token the privClient is based on will expire.
     */
    private static long privClientExpiration = -1;

    static S3Client s3Client;

    public Globus()
    {
    }

    public Globus(GlobusAuthToken authToken)
    {
        try
        {
            client = createClient(authToken);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public GlobusClient getClient()
    {
        return client;
    }

    private static GlobusClient createClient(GlobusAuthToken authToken)
    {
        GlobusClient newClient = null;
        if (authToken != null)
        {
            try
            {
                newClient = new GlobusClient(authToken);
            }
            catch (Exception e)
            {
                logger.error("Error creating GlobusClient with token '"
                        + authToken + "' because \n"
                        + ExceptionUtils.getStackTrace(e));
            }

            newClient.setRootUrlForRequestType(RequestType.transfer,
                    getGlobusConfigProperty(GLOBUS_TRANSFER_API_PROP));
            newClient.setRootUrlForRequestType(RequestType.groups,
                    getGlobusConfigProperty(GLOBUS_GROUP_API_PROP));
            newClient.setRootUrlForRequestType(RequestType.auth,
                    getGlobusConfigProperty(GLOBUS_IDENTITY_API_PROP));
        }
        return newClient;
    }

    public static String getPublishDashboardPath()
    {
        return Globus.PUBLISH_DASHBOARD_PATH;
    }

    public static String getPrefixedGroupName(String gpId, String gpName)
    {
        return Globus.GLOBUS_GROUP_PREFIX + gpId;
    }

    public static Configurable getGlobusStorageCollectionConfig()
    {
        return new BaseConfigurable("Dataset Storage",
                new ConfigurableProperty(Globus.CONFIG_GLOBUS_ENDPOINT_NAME,
                        "Endpoint Identifier", "", DataType.STRING,
                        "UUID Representation", true),
                new ConfigurableProperty(Globus.CONFIG_GLOBUS_SHARE_PATH_PREFIX,
                        "Collection Root Path",
                        getGlobusConfigProperty(
                                DEFAULT_CONFIG_GLOBUS_SHARE_PATH_PREFIX),
                        DataType.STRING, "Root path to dataset storage", true));
    }

    public static String getGlobusGroupName(GlobusClient client, String gpName)
    {
        try
        {
            GlobusGroup globusGroup = client
                    .getGroupById(Globus.getUnPrefixedGroupID(gpName));
            return globusGroup.name;
        }
        catch (Exception e)
        {
            logger.error("Error getting Globus Group " + e);
        }
        return null;
    }

    public static String getUnPrefixedGroupID(String gpName)
    {
        if (!isGlobusGroup(gpName))
        {
            return gpName;
        }
        String[] parts = gpName.split(Globus.GLOBUS_GROUP_PREFIX);
        if (parts.length < 2)
        {
            return gpName;
        }

        return parts[1];
    }

    public static boolean isGlobusGroup(String gpName)
    {
        if (gpName == null)
        {
            return false;
        }
        else
        {
            return gpName.startsWith(Globus.GLOBUS_GROUP_PREFIX);
        }

    }

    public static String getGlobusBaseURL()
    {
        return getGlobusConfigProperty(GLOBUS_URL_PROP);
    }

    public static String getGlobusGroupLink(String gpID, int length)
    {
        GlobusClient privClient = getPrivlegedClient();

        String globusFullGroupName = getGlobusGroupName(privClient, gpID);
        String globusGroupName = globusFullGroupName;
        if (length > 0 && globusGroupName != null
                && globusGroupName.length() > length)
        {
            globusGroupName = globusGroupName.substring(0, length) + " ...";
        }

        return "<a href='" + getGlobusConfigProperty(GLOBUS_URL_PROP)
                + "/Groups?id=" + Globus.getUnPrefixedGroupID(gpID)
                + "' title='" + globusFullGroupName + "'target=\"_blank\">"
                + globusGroupName + "</a>";
    }

    public static String encodeEndpointPath(String endpoint, String path)
    {
        return "globus://" + endpoint + ":" + path;
    }

    public static String getFileNameFromURI(String name)
    {
        if (!name.startsWith("globus://"))
        {
            return name;
        }
        int i = name.lastIndexOf(":/");
        if (i < 0)
        {
            return name;
        }
        return name.substring(i + 2);
    }

    public static String getCollectionConfigurationBannerMsg()
    {
        return getGlobusConfigProperty(GLOBUS_CONFIG_COLLECTION_BANNER_MSG);
    }

    public static String getGlobusConfigProperty(String propName)
    {
        String propVal = ConfigurationManager.getProperty(GLOBUS_CONFIG_MODULE,
                propName);
        if (propVal == null)
        {
            logger.error(
                    "No configuration value found for property " + propName);
        }
        return propVal;
    }

    public static boolean getGlobusConfigBooleanProperty(String propName)
    {
        return ConfigurationManager.getBooleanProperty(GLOBUS_CONFIG_MODULE,
                propName);
    }

    public static String getGroupSelectPage(int dspaceID,
            String dspaceRedirectURL, String collectionHandle,
            String communityHandle, boolean skipGroups)
    {
        String groupsUrl = getGlobusConfigProperty(GLOBUS_URL_PROP)
                + getGlobusConfigProperty(GLOBUS_GROUPS_URL_CONFIG_PROP);

        String collectionAndCommunity = "";
        if (collectionHandle != null)
        {
            collectionAndCommunity += "&collection_handle=" + collectionHandle;
        }
        if (communityHandle != null)
        {
            collectionAndCommunity += "&community_handle=" + communityHandle;
        }
        if (skipGroups)
        {
            collectionAndCommunity += "&skipGroupsPage=true";
        }
        String actionPart = GlobusClient
                .utf8Encode(dspaceRedirectURL + "?group_id=" + dspaceID
                        + "&globusCallback=true" + collectionAndCommunity);

        if (groupsUrl != null && actionPart != null)
        {
            return groupsUrl + "?action=" + actionPart;
        }
        return null;
    }

    public static String getGlobusLogoutLink(HttpServletRequest request)
    {
        String redirectUrl = getPublishURL(request) + "/login/logged-out.jsp";
        String logoutURL = getGlobusConfigProperty(GLOBUS_AUTH_URL_PROP)
                + getGlobusConfigProperty(LOGOUT_URL_CONFIG_PROP);
        return logoutURL + "?redirect_uri="
                + GlobusClient.utf8Encode(redirectUrl);
    }

    public static String getTransferPage(String epName, String path)
    {
        epName = GlobusClient.utf8Encode(epName);
        String transferBaseUrl = getGlobusConfigProperty(GLOBUS_URL_PROP)
                + getGlobusConfigProperty(GLOBUS_TRANSFER_URL_CONFIG_PROP);
        if (transferBaseUrl != null && epName != null)
        {
            transferBaseUrl = transferBaseUrl + "?destination_id=" + epName
                    + "&destination_path=" + path;
            return transferBaseUrl;
        }
        return null;
    }

    public static String getTransferActivityPage(String taskId) {
        return getGlobusConfigProperty(GLOBUS_URL_PROP)  + "/app/activity/"
                + taskId + "/overview";  
    }

    public static String getTransferURL(String epName, String path)
    {
        epName = GlobusClient.utf8Encode(epName);
        String transferUrl = getGlobusConfigProperty(GLOBUS_URL_PROP)
                + getGlobusConfigProperty(GLOBUS_TRANSFER_URL_CONFIG_PROP);
        if (transferUrl != null && epName != null)
        {
            return (transferUrl + "?origin_id=" + epName + "&origin_path="
                    + path);
        }
        return null;
    }

    public static String getPublicationLeafDirPrefix()
    {
        return getGlobusConfigProperty(GLOBUS_PUBLICATION_DIR_PREFIX);
    }

    public static String getPathToData(Item item)
    {
        String globusSharePath = item.getGlobusSharePath();
        String dataDirName = getGlobusConfigProperty(
                PUBLICATION_DATA_DIRECTORY);
        if (dataDirName == null)
        {
            return globusSharePath;
        }
        else
        {
            if (!globusSharePath.endsWith("/"))
            {
                globusSharePath = globusSharePath + "/";
            }
            return globusSharePath + dataDirName;
        }
    }

    public GlobusUser getUserProfile(String userName, String token)
            throws GlobusClientException
    {
        return client.getUser(userName);
    }

    public static String getPublishURL(HttpServletRequest request)
    {
        String port = "";
        if (request.getServerPort() != 80 && request.getServerPort() != 443)
        {
            port = ":" + request.getServerPort();
        }
        return request.getScheme() + "://" + request.getServerName() + port
                + request.getContextPath();
    }

    /**
     * Get the URL to redirect to to perform GOAuth authentication, followed by
     * a re-direct back to the page initially requested.
     *
     * @param request
     *            The request object on the access that requires GOAuth
     *            authentication prior to accessing the page.
     * @return The URL (as a String) that can be accessed to perform GOAuth
     *         authentication with a return back to this page following a
     *         successful authentication.
     */
    public static String getGoAuthRedirectUrl(HttpServletRequest request)
    {
        String requestUrl = getPublishURL(request) + "/goauth-login";
        String clientId = getGlobusConfigProperty(
                GLOBUS_GOAUTH_CLIENTID_CONFIG_PROP);
        return String.format(getGlobusConfigProperty(GLOBUS_URL_PROP)
                + "/OAuth?response_type=code&client_id=%s" + "&redirect_uri=%s",
                clientId, requestUrl);
    }

    public static String encodeSharedEndpointName(String endpoint)
    {
        return GlobusClient.utf8Encode(endpoint);
    }

    // get the shared ep name e.g., go#ep1
    public static String getSharedEndpointName(Context context,
            Collection collection)
    {
        return GlobusConfigurationManager.getProperty(context,
                Globus.CONFIG_GLOBUS_ENDPOINT_NAME, null, collection);
    }

    /**
     * Fix up paths to satisfy input requirements. This includes removing any
     * double slashes in the path name and, optionally, insuring that leading or
     * trailing slashes are in place.
     *
     * @param inputPath
     *            The path to fix up
     * @param leadingSlash
     *            if {@code true} the returned path will be guaranteed to have a
     *            leading slash. If {@code false} no guarantee is made.
     * @param trailingSlash
     *            if {@code true} the returned path will be guaranteed to have
     *            an ending slash. If {@code false} no guarantee is made.
     * @return A path string cleaned up as required.
     */
    public static String fixPath(String inputPath, boolean leadingSlash,
            boolean trailingSlash)
    {
        // Remove any instances of double (or triple, etc.) / with single /
        while (inputPath.contains("//"))
        {
            inputPath = inputPath.replace("//", "/");
        }
        if (leadingSlash && !inputPath.startsWith("/"))
        {
            inputPath = "/" + inputPath;
        }
        if (trailingSlash && !inputPath.endsWith("/"))
        {
            inputPath = inputPath + "/";
        }
        return inputPath;
    }

    private static String getPathToPubUnpubRootDir(Context context,
            Collection collection, String dirLocationProp)
    {
        // get the shared EP path for this item
        String sharePrefix = GlobusConfigurationManager.getProperty(context,
                Globus.CONFIG_GLOBUS_SHARE_PATH_PREFIX, null, collection);
        String pubOrUnpubDirName = getGlobusConfigProperty(dirLocationProp);
        if (sharePrefix != null && pubOrUnpubDirName != null)
        {
            return fixPath(sharePrefix + "/" + pubOrUnpubDirName, true, false);
        }
        return null;

    }

    public static String getPathToUnpublishedDir(Context context,
            Collection collection)
    {
        return getPathToPubUnpubRootDir(context, collection,
                GLOBUS_INPROGRESS_DIR_PROP);
    }

    public static String getPathToPublishedDir(Context context,
            Collection collection)
    {
        return getPathToPubUnpubRootDir(context, collection,
                GLOBUS_COMPLETED_DIR_PROP);

    }

    private static String getPathToDataset(Context context, String id,
            Collection collection, String dirLocationProp)
    {
        String sharePrefix = getPathToPubUnpubRootDir(context, collection,
                dirLocationProp);
        String dataSetDirPrefix = getPublicationLeafDirPrefix();

        if (sharePrefix != null && dataSetDirPrefix != null)
        {
            // Globus ACLs require / before and after directory name
            return fixPath(sharePrefix + "/" + dataSetDirPrefix + id, true,
                    true);
        }
        else
        {
            return null;

        }
    }

    // get the shared path e.g., /sharepath/
    public static String getUnpublishedPath(Context context, String id,
            Collection collection)
    {
        return getPathToDataset(context, id, collection,
                GLOBUS_INPROGRESS_DIR_PROP);
    }

    public static String getPublishedPath(Context context, String id,
            Collection collection)
    {
        return getPathToDataset(context, id, collection,
                GLOBUS_COMPLETED_DIR_PROP);
    }

    // Mappings of dublin core to datacite so that we create fully datacite
    // JSONLD files
    private static final Properties dublinCoreDataciteMap = new Properties();
    static
    {
        loadMetadataMap(dublinCoreDataciteMap);
    }

    public static void loadMetadataMap(Properties map)
    {
        String fname = getGlobusConfigProperty(GLOBUS_METADATA_MAPPING_PROP);
        logger.info("Loading metadata map from file " + fname);
        InputStream input = null;
        try
        {
            input = new FileInputStream(fname);
            map.load(input);
            logger.info("Loaded " + map.size() + " mappings from file");
        }
        catch (Exception e)
        {
            logger.error("Exception reading metadata mapping file " + fname);
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (Exception ex)
                {
                    logger.error("Exception closing input stream");
                }
            }
        }
    }

    public static String makeJSONLDKey(DCValue dcval)
    {
        return Globus.makeJSONLDKey(dcval.schema, dcval.element,
                dcval.qualifier);
    }

    public static String getDelimiter()
    {
        if (dublinCoreDataciteMap.containsKey("delimiter"))
        {
            return dublinCoreDataciteMap.getProperty("delimiter");
        }
        return "/";
    }

    public static String makeJSONLDKey(String schema, String element,
            String qualifier)
    {
        if (qualifier == null)
        {
            return schema + ":" + element;
        }
        else
        {
            return schema + ":" + element + getDelimiter() + qualifier;
        }
    }

    public static String getAllMetadata(Context context, Item item,
            Collection collection)
    {
        try
        {
            JSONObject dataset = new JSONObject();
            if (collection == null)
            {
                collection = item.getOwningCollection();
            }
            Community community = collection.getCommunities()[0];
            EPerson person = item.getSubmitter();

            dataset.put("_comment", "This file was auto-generated by Globus");
            dataset.put(
                    Globus.makeJSONLDKey("globus", "publication", "submitter"),
                    person.getGlobusUserName());
            dataset.put(
                    Globus.makeJSONLDKey("globus", "publication", "collection"),
                    collection.getName());
            dataset.put(Globus.makeJSONLDKey("globus", "publication",
                    "collection_id"), collection.getID());
            dataset.put(
                    Globus.makeJSONLDKey("globus", "publication", "community"),
                    community.getName());
            dataset.put(Globus.makeJSONLDKey("globus", "publication",
                    "community_id"), community.getID());
            dataset.put(
                    Globus.makeJSONLDKey("globus", "publication", "item_id"),
                    item.getID());

            Set<String> schemas = new HashSet<String>();
            schemas.add("globus");
            String key;
            DCValue[] dcv = item.getMetadata(Item.ANY, Item.ANY, Item.ANY,
                    Item.ANY);
            for (DCValue dcval : dcv)
            {
                // Make a unique key for this piece of metadata
                // if its a DC term we map to datacite
                if (dublinCoreDataciteMap.containsKey(dcval.getField()))
                {
                    key = dublinCoreDataciteMap.getProperty(dcval.getField());
                    schemas.add("datacite");
                }
                else
                {
                    key = Globus.makeJSONLDKey(dcval);
                    schemas.add(dcval.schema);
                }
                // We assume all metadata values could be multivalued
                // There is no way to tell so we encode all in an array
                JSONArray valueArray = null;
                if (!dataset.has(key))
                {
                    valueArray = new JSONArray();
                }
                else
                {
                    valueArray = dataset.getJSONArray(key);
                }
                valueArray.put(dcval.value);
                dataset.put(key, valueArray);
            }
            // create JSON-LD context heading
            JSONObject jsonLDContext = new JSONObject();
            MetadataSchema metadataSchema;
            for (String s : schemas)
            {
                metadataSchema = MetadataSchema.find(context, s);
                jsonLDContext.put(s, metadataSchema.getNamespace());
            }
            dataset.put("@context", jsonLDContext);
            return dataset.toString();
        }
        catch (Exception e)
        {
            logger.error("Error getting item metadata " + e);
        }
        return "";
    }

    public static boolean waitForTransferCompletion(GlobusClient globusClient,
            String taskId)
    {
        logger.info("Waiting for transfer completion " + taskId);
        // TaskId empty string as returned by createArtifactFile signals that no
        // transfer was
        // to be done, so we're in good shape
        if ("".equals(taskId))
        {
            return true;
        }
        int waitTime = Integer
                .parseInt(getGlobusConfigProperty(TRANSFER_WAIT_TIME_PROP));

        if (taskId == null)
        {
            logger.error("TaskID is null, cannot wait for transfer completion");
            return false;
        }

        Task requestTask = new Task();
        requestTask.taskId = taskId;
        requestTask.status = Task.Status.SUCCEEDED;

        TaskList taskList;
        for (int i = 0; i < waitTime; i++)
        {
            try
            {
                taskList = globusClient.taskList(requestTask, null, null, null);
                if (taskList.length > 0)
                {
                    return true;
                }
            }
            catch (Exception e)
            {
            }
            try
            {
                Thread.sleep(1000); // sleep for one second.
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    public static String transferFiles(GlobusClient globusClient,
            String sourceEP, String sourcePath, String destEP, String destPath,
            String label) throws Exception
    {
        globusClient.activateEndpoint(sourceEP);
        globusClient.activateEndpoint(destEP);

        TransferJob tj = new TransferJob(globusClient);
        tj.sourceEndpoint = sourceEP;
        tj.destinationEndpoint = destEP;
        tj.label = label;
        TransferItem ti = new TransferItem(sourcePath, destPath, false);
        if (sourcePath.endsWith("/"))
        {
            ti.destPath = destPath + sourcePath;
            ti.recursive = true;
        }

        logger.info("Starting transfer from " + sourceEP + " path " + sourcePath
                + " == " + destEP + " path " + destPath + " label " + label);

        tj.addTransferItem(ti);

        Task transferTask = tj.execute();
        if (transferTask != null)
        {
            return transferTask.taskId;
        }

        return null;
    }

    public boolean autoActivate(String endpointName)

    {
        if (client == null)
        {
            return false;
        }
        try
        {
            return client.activateEndpoint(endpointName);
        }
        catch (GlobusClientException e)
        {
            logger.error("Failed to activate endpoint " + endpointName, e);
            return false;
        }
    }

    public static String escapeTransferLabel(String in)
    {
        // Transfer doesn't let us have a #,/ in a label
        // It also doesn' let us use % so URLEncoder is out
        in = in.replace("#", "-");
        in = in.replace("/", "-");
        return in;
    }

    /**
     * @return
     */
    public static String getFormConfigDir()
    {
        String dirName = getGlobusConfigProperty(GLOBUS_FORMS_DIR_CONFIG_PROP);
        return dirName;
    }

    /**
     * @return
     */
    public static String getWorkflowConfigDir()
    {
        String dirName = getGlobusConfigProperty(
                GLOBUS_WORKFLOW_DIR_CONFIG_PROP);
        return dirName;
    }

    public static Globus getGlobusClientFromRequest(HttpServletRequest request)
    {
        Context context = (Context) request.getAttribute("dspace.context");
        Globus g = null;
        if (context != null)
        {
            g = getGlobusClientFromContext(context);
            if (g == null)
            {
                HttpSession session = request.getSession(false);
                if (session != null)
                {
                    GlobusAuthToken authToken = (GlobusAuthToken) session
                            .getAttribute(GLOBUS_AUTH_TOKEN_SESSION_ATTR_NAME);
                    if (authToken != null)
                    {
                        g = new Globus(authToken);
                    }
                }
            }
        }

        return g;
    }

    public static Globus getGlobusClientFromContext(Context context)
    {
        if (context == null)
        {
            return null;
        }
        EPerson currentUser = context.getCurrentUser();
        int personId = -1;
        if (currentUser != null)
        {
            personId = currentUser.getID();
        }
        else
        {
            logger.warn("No user for context: " + context);
        }
        Globus g = (Globus) context.fromCache(Globus.class, personId);
        if (g == null)
        {
            GlobusAuthToken authToken = (GlobusAuthToken) context
                    .fromCache(GlobusAuthToken.class, personId);
            if (authToken != null)
            {
                g = new Globus(authToken);
                addGlobusClientToContext(g, context);
            }
        }
        return g;
    }

    public static void addGlobusClientToContext(Globus g, Context context)
    {
        EPerson eperson = context.getCurrentUser();
        int personId = -1;
        if (eperson != null)
        {
            personId = eperson.getID();
        }
        else
        {
            logger.warn("No person for context: " + context);
        }
        context.cache(g, personId);
    }

    /**
     * @return
     */
    public synchronized static GlobusClient getPrivlegedClient()
    {
        if (privClient == null || System.currentTimeMillis() > privClientExpiration)
        {
            GlobusAuthToken authToken = null;
            String privUserName = ConfigurationManager.getProperty(
                    GLOBUS_AUTH_CONFIG_MODULE, GLOBUS_USER_CONFIG_PROP);
            String privPassword = ConfigurationManager.getProperty(
                    GLOBUS_AUTH_CONFIG_MODULE, GLOBUS_USER_PWD_CONFIG_PROP);
            if (privUserName != null && privPassword != null)
            {
                authToken = GlobusAuth.getAuthTokenFromUsernamePassword(privUserName, privPassword);
            }

            if (authToken == null) {                
                String privTokenJson = ConfigurationManager.getProperty(GLOBUS_AUTH_CONFIG_MODULE,
                        GLOBUS_USER_TOKEN_CONFIG_PROP);
                try
                {
                    authToken = GlobusAuthToken.fromJson(privTokenJson);
                }
                catch (IOException e)
                {
                    logger.warn("Failed to create AuthToken from Json: " + privTokenJson);
                }
            }

            if (authToken != null)
            {
                privClient = createClient(authToken);
                // Estimate expiration as the token lifetime minus an hour for fudge factor
                // and in case there's no call for an extended period, etc.
                privClientExpiration = System.currentTimeMillis()
                        + (authToken.tokenLife * 1000L) - (3600 * 1000);
            }
        }
        return privClient;
    }

    /**
     * Cache a copy of an access token from an HttpServletRequest object in to
     * the provided context.
     *
     * @param request
     * @param context
     */
    public static void copyTokenRequestToContext(HttpServletRequest request,
            Context context)
    {
        if (request == null || context == null)
        {
            logger.warn("copyTokenRequestToContext called with null parameter");
            return;
        }
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null)
        {
            // Is this a common case, or is it exceptional enough that it should
            // be logged?
            return;
        }
        // We don't want to create a session if there isn't one
        HttpSession session = request.getSession(false);
        if (session != null)
        {
            GlobusAuthToken authToken = (GlobusAuthToken) session
                    .getAttribute(GLOBUS_AUTH_TOKEN_SESSION_ATTR_NAME);
            if (authToken != null)
            {
                context.cache(authToken, currentUser.getID());
            }
        }

    }

    /**
     * @param request
     * @param authToken
     */
    public static void addTokenToHttpSession(HttpServletRequest request,
            GlobusAuthToken authToken)
    {
        if (authToken == null || request == null)
        {
            logger.warn("addTokenToHttpSession called with null parameter");
            return;
        }

        // AccessToken token = new AccessToken(accessToken);
        HttpSession session = request.getSession();
        if (session != null)
        {
            session.setAttribute(GLOBUS_AUTH_TOKEN_SESSION_ATTR_NAME,
                    authToken);
        }
    }

    /**
     * @return
     */
    public static int getServerId()
    {
        String serverIdString = getGlobusConfigProperty(GLOBUS_SERVER_ID_PROP);
        return Integer.parseInt(serverIdString);
    }
}
