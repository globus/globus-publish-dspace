package org.dspace.globus;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.globus.GlobusClient;

public class GlobusWebAppIntegration
{
    private static final Logger logger = Logger.getLogger(GlobusWebAppIntegration.class);

    private static final String GLOBUS_WEBAPP_BASE_PROPNAME = "globus.webapp.url";

    private static final String GLOBUS_WEBAPP_FILEMANAGER_URL_TEMPLATE_PROPNAME = "filemanager.templated.path";
    private static final String GLOBUS_WEBAPP_ACTIVITY_URL_TEMPLATE_PROPNAME = "activity.templated.path";

    private static final String GLOBUS_GROUP_PICKER_PATH_PROPNAME = "group.picker.path";
    private static final String GLOBUS_WEBAPP_GROUP_URL_TEMPLATE_PROPNAME = "group.templated.path";

    private static String templateReplacement(String template,
                                              Map<String, String> templateVals)
    {
        if (template == null) {
            return null;
        }
        for (String key : templateVals.keySet()) {
            String val = templateVals.get(key);
            // Loop here because replaceAll treats the pattern as a regex, and
            // that may very well mess with the use of the '$' indicator
            // And, here's a chance to be paranoid about a missing val in the
            // Map.
            while (template.contains("$"+ key) && val != null) {
                template = template.replace("$" + key, val);
            }
        }
        return template;
    }

    public static String getGroupSelectionUrl(int dspaceID,
            String dspaceRedirectURL, String collectionHandle,
            String communityHandle, boolean skipGroups)
    {
        String groupsUrl = Globus.getGlobusConfigProperty(GLOBUS_WEBAPP_BASE_PROPNAME)
                + Globus.getGlobusConfigProperty(GLOBUS_GROUP_PICKER_PATH_PROPNAME);

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
                            + "&globusCallback=true&multiple=no" +
                            collectionAndCommunity);

        if (groupsUrl != null && actionPart != null)
        {
            logger.info("Returning group selection url: " + 
                        (groupsUrl + "?action=" + actionPart));
            return groupsUrl + "?action=" + actionPart;
        }
        return null;
    }

    public static String getGroupWebAppUrl(String groupId)
    {
        String groupWebAppUrlTemplate =
            Globus.getGlobusConfigProperty(GLOBUS_WEBAPP_BASE_PROPNAME) +
            Globus.getGlobusConfigProperty(GLOBUS_WEBAPP_GROUP_URL_TEMPLATE_PROPNAME);
        Map<String, String> templateProps = new HashMap<>();
        templateProps.put("groupId", groupId);
        String groupWebAppUrl = templateReplacement(groupWebAppUrlTemplate,
                                                    templateProps);
        return groupWebAppUrl;        
    }

    public static String getGlobusGroupHrefTag(String gpID, int maxLength)
    {
        GlobusClient privClient = Globus.getPrivlegedClient();

        String globusFullGroupName = Globus.getGlobusGroupName(privClient, gpID);
        String globusGroupName = globusFullGroupName;
        if (maxLength > 0 && globusGroupName != null
                && globusGroupName.length() > maxLength)
        {
            globusGroupName = globusGroupName.substring(0, maxLength) + " ...";
        }

        String groupWebAppUrl = getGroupWebAppUrl(gpID);
        return "<a href=\"" + groupWebAppUrl +
            "\" title=\"" + globusFullGroupName + 
            "\" target=\"_blank\">" + globusGroupName + "</a>";
    }

    /**
     * Get the URL for the Globus WebApp for interacting with the FileManager 
     * (Transfer).
     * @param epName The Name or the id of the endpoint to use
     * @param path The path at the endpoint to select
     * @param source must be either "origin" or "destination" indicating whether
     * the origin or destination should be seeded in the filemanager page.
     */
    public static String getWebAppFileManagerUrl(String epName, String path,
                                                 String source)        
    {
        epName = GlobusClient.utf8Encode(epName);
        String transferBaseUrlTemplate =
            Globus.getGlobusConfigProperty(GLOBUS_WEBAPP_BASE_PROPNAME)
            + Globus.getGlobusConfigProperty(GLOBUS_WEBAPP_FILEMANAGER_URL_TEMPLATE_PROPNAME);

        Map<String, String> templateProps = new HashMap<>();
        templateProps.put("destination_id", epName);
        templateProps.put("destination_path", path);
        templateProps.put("source", source);
        String transferWebAppUrl = templateReplacement(transferBaseUrlTemplate,
                                                       templateProps);
        return transferWebAppUrl;
    }

    public static String getWebAppActivityUrl(String taskId) {
        String activityUrlTemplate = Globus.getGlobusConfigProperty(GLOBUS_WEBAPP_BASE_PROPNAME)
            + Globus.getGlobusConfigProperty(GLOBUS_WEBAPP_ACTIVITY_URL_TEMPLATE_PROPNAME);
        Map<String, String> templateProps = new HashMap<>();
        templateProps.put("task_id", taskId);
        String activityWebAppUrl = templateReplacement(activityUrlTemplate,
                                                       templateProps);
        return activityWebAppUrl;
    }
}
