/*
 * Copyright 2014 University of Chicago.
 * All Rights Reserved.
 */
package org.globus;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.globus.GlobusRestInterface.RequestType;
import org.globus.auth.AuthInterface;
import org.globus.auth.GlobusAuthToken;
import org.globus.auth.GlobusUser;
import org.globus.auth.Identity;
import org.globus.groups.GlobusGroup;
import org.globus.groups.GlobusGroupMembership;
import org.globus.groups.GlobusGroupSummary;
import org.globus.groups.GroupMember;
import org.globus.groups.GroupsInterface;
import org.globus.groups.GroupsInterface.MembershipRole;
import org.globus.groups.GroupsInterface.MembershipStatus;
import org.globus.transfer.Endpoint;
import org.globus.transfer.SuccessfulTransfer;
import org.globus.transfer.SuccessfulTransfers;
import org.globus.transfer.Task;
import org.globus.transfer.TaskList;
import org.globus.transfer.TransferAccess;
import org.globus.transfer.TransferAccessList;
import org.globus.transfer.TransferDirectoryListing;
import org.globus.transfer.TransferInterface;
import org.globus.transfer.TransferInterface.PrincipalType;
import org.globus.transfer.TransferJob;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;


/**
 * @author pruyne
 *
 */
public class GlobusClientTest
{
    /**
     * 
     */
//    private static final String TEST_GROUP_ID = "373a2b88-6c85-11e2-95ce-12313809f035";
    private static final String TEST_GROUP_ID = "c7dfa7a4-eab9-11e3-ad3f-22000ab68755";
    private static final String TEST_SHARING_USER = "jimdemo";
    private static final String TEST_TEMPDIR = "/testDir/";
    private static final String TEST_ENDPOINT = "go#ep1";
    private static final String TEST_USER = "jimdemo";
    private static final String TEST_USER_FULLNAME = "jimdemo@globusid.org";
    private static final String TEST_USER_ID_UUID = "a8712308-d274-11e5-a5bd-0b7315ad5bc0";
    private static final String TEST_PASSWORD = "";

    private static final String TOKEN_FILE = "bearerTokenList.txt";
    private static final String AUTH_TOKEN_FILE = "access_token.json";
    
    private static final String IDENTITY_HOST = "https://auth.globus.org/v2/api";
    private static final String TRANSFER_HOST = "https://transfer.api.globusonline.org/v0.10";     
    private static final String GROUPS_HOST = "https://nexus.api.globusonline.org";

    private static GlobusClient client;

    @BeforeClass
    public static void initClientBearerList() throws IOException
    {
        InputStream tokenStream =
            GlobusClientTest.class.getClassLoader().getResourceAsStream(TOKEN_FILE);
        if (tokenStream != null) {
            client = new GlobusClient(tokenStream);
            tokenStream.close();
        }
        addUrlsToClient();
    }

    private static void addUrlsToClient()
    {
        client.setRootUrlForRequestType(RequestType.groups, GROUPS_HOST);
        client.setRootUrlForRequestType(RequestType.auth, IDENTITY_HOST);
        client.setRootUrlForRequestType(RequestType.transfer, TRANSFER_HOST);
    }
    
//    @BeforeClass
    public static void initClientTokenFile() throws IOException
    {
        GlobusAuthToken authToken = authTokenFromResource(AUTH_TOKEN_FILE);
        client = new GlobusClient(authToken);
        addUrlsToClient();
    }    
    
    private static String resourceFileToString(String resourceName) throws IOException
    {
        InputStream is = GlobusClientTest.class.getClassLoader().getResourceAsStream(resourceName);
        try {
            return streamToString(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }


    /**
     * @param is
     * @return
     * @throws IOException 
     */
    private static String streamToString(InputStream is) throws IOException
    {
        StringBuffer sbuf = new StringBuffer();
        byte[] buf = new byte[1024];
        int numRead = 0;
        while (is != null && (numRead = is.read(buf)) > 0) {
            sbuf.append(new String(buf, 0, numRead));
        }
        return sbuf.toString();
    }

    private static GlobusAuthToken authTokenFromResource(String resourceName) throws IOException
    {
        String tokenJson = resourceFileToString(resourceName);
        GlobusAuthToken authToken = GlobusAuthToken.fromJson(tokenJson);
        return authToken;
    }
    
    @Test
    public void testGlobusAuthTokenFromFile() throws IOException
    {
        GlobusAuthToken authToken = authTokenFromResource(AUTH_TOKEN_FILE);
        assertNotNull(authToken);
    }
    
    /**
     * Test method for {@link org.globus.nexus.GoauthClient#getClientOnlyAccessToken()}.
     * @throws GlobusClientException
     */
    @Test
    @Category(AuthInterface.class)
    public void testGetClientOnlyAccessToken() throws GlobusClientException
    {
        GlobusAuthToken authToken = client.getAuthTokenForRequestType(RequestType.auth);
        String accessToken = authToken.tokenValue;
        assertNotNull(accessToken);
        System.out.println(accessToken);
    }

    
    @Test
    @Category(AuthInterface.class)
    public void testGetCurrentUser() throws GlobusClientException
    {
        GlobusUser activeUser = client.getUser(TEST_USER);
        assertNotNull(activeUser);
        assertEquals(TEST_USER, activeUser.username);
        System.out.println(activeUser);
    }

    @Test
    @Category(AuthInterface.class)
    public void testGetIdentities() throws GlobusClientException
    {
        List<Identity> identities = client.getIdentitiesForUsernames(new String[] { TEST_USER_FULLNAME}, null, true);
        assertNotNull(identities);
        assertEquals(identities.get(0).username, TEST_USER_FULLNAME);
        System.out.println("Got identities: " + identities);
    }

    @Test
    @Category(AuthInterface.class)
    public void testGetIdentitiesByIds() throws GlobusClientException
    {
        String identity_id = TEST_USER_ID_UUID;
        List<Identity> identities = client.getIdentitiesForUserIds(new String[] { identity_id }, null, true);
        assertNotNull(identities);
        assertEquals(identities.get(0).id, TEST_USER_ID_UUID);
        System.out.println("Got from id identities: " + identities);
    }
    
    
    @Test
    @Category(AuthInterface.class)
    public void testGetUserIds() throws GlobusClientException
    {
        Map<String, String> nameMap = client.getUniqueIdForUserNames(new String[] {TEST_USER_FULLNAME});
        System.out.println("Got back map: " + nameMap);
    }

    @Test
    @Category(GroupsInterface.class)
    public void testGetAllGroups() throws GlobusClientException
    {
        GlobusGroupSummary[] groups = client.getAllGroups();
        assertNotNull(groups);
        assertNotEquals(0, groups.length);
        GlobusGroupSummary[] groupsAgain = client.getAllGroups();
        String groupsString = ((Object) groups).toString();
        String againString = ((Object) groupsAgain).toString();
        assertEquals(groupsString, againString);
    }


    @Test
    @Category(GroupsInterface.class)
    public void testGetGroupById() throws GlobusClientException
    {
        String groupId = TEST_GROUP_ID;
        GlobusGroup group = client.getGroupById(groupId);
        assertNotNull(group);
        System.out.println(group);
        Object members = group.getMembers();
        System.out.println("Got back Members: " + members);
    }


    @Test
    @Category(GroupsInterface.class)
    public void testGetGroupMembers() throws GlobusClientException
    {
        String groupId = TEST_GROUP_ID;
        Collection<GroupMember> members = client.getMembersForGroup(groupId, 
                                                                    EnumSet.of(MembershipRole.admin, 
                                                                               MembershipRole.member), 
                                                                    EnumSet.of(MembershipStatus.active), 
                                                                    null);
        assertNotNull(members);
        System.out.println("Members: " + members);
        Collection<GroupMember> members2 = client.getMembersForGroup(groupId, 
                                                                     EnumSet.of(MembershipRole.admin, 
                                                                                MembershipRole.member), 
                                                                     EnumSet.of(MembershipStatus.active), 
                                                                     null);
        assertEquals(members, members2);
        
        for (GroupMember member : members) {
            GlobusUser user = member.getUserDetails();
            assertNotNull(user);
            System.out.println(("Detailts on User: " + user));
        }
    }


    @Test
    @Category(GroupsInterface.class)
    public void testGetGroupMembersWithStatus() throws GlobusClientException
    {
        String groupId = TEST_GROUP_ID;
        Collection<GroupMember> members =
            client.getMembersForGroup(groupId, null, EnumSet.of(MembershipStatus.active), TEST_USER);
        assertNotNull(members);
        assertTrue(members.size() > 0);
        boolean foundmember = false;
        for (GroupMember member : members) {
            if (TEST_USER.equals(member.username)) {
                foundmember = true;
                System.out.println("Found user " + TEST_USER + " (by status & name): " + member);
                break;
            }
        }
        assertTrue(foundmember);
    }


    @Test
    @Category(GroupsInterface.class)
    public void testGetGroupMembersWithRole() throws GlobusClientException
    {
        String groupId = TEST_GROUP_ID;
        Collection<GroupMember> members =
            client.getMembersForGroup(groupId, EnumSet.of(MembershipRole.admin), null, null);
        assertNotNull(members);
        assertTrue(members.size() == 1);
        boolean foundadmin = false;
        for (GroupMember member : members) {
            if (TEST_USER.equals(member.username)) {
                foundadmin = true;
                System.out.println("Found admin user (by role): " + member);
                GlobusUser userDetails = member.getUserDetails();
                assertNotNull(userDetails);
                System.out.println("Details for user: " + userDetails);
                break;
            }
        }
        assertTrue(foundadmin);
    }

    @Test
    @Category(GroupsInterface.class)
    public void testGetGroupsForCurrentUser() throws GlobusClientException
    {
        GlobusGroupSummary[] summaries = client.getGroupsForCurrentUser();
        assertNotNull(summaries);
        for (GlobusGroupSummary summary : summaries) {
            GlobusGroup group = summary.getGroup();
            assertNotNull(group);
        }
        // Test caching
        GlobusGroupSummary[] summaries2 = client.getGroupsForCurrentUser();
        assertTrue(summaries.equals(summaries2));
    }

    @Test
    @Category(GroupsInterface.class)
    public void testGetGroupsForUser() throws GlobusClientException
    {
        Collection<GlobusGroupMembership> groupsForUser = client.getGroupsForUser(TEST_USER);
        assertNotNull(groupsForUser);
        assertFalse(groupsForUser.isEmpty());
        System.out.println("User's groups: " + groupsForUser);
    }

    
    @Test
    @Category(TransferInterface.class)
    public void testActivateEndpoint() throws GlobusClientException
    {
        boolean success = client.activateEndpoint(TEST_ENDPOINT);
        assertTrue(success);
    }


    @Test
    @Category(TransferInterface.class)
    public void testLsEndpoint() throws GlobusClientException
    {
        TransferDirectoryListing dir = client.lsDirectory(TEST_ENDPOINT, "/share/godata");
        assertNotNull(dir);
        System.out.println(dir);
    }


    @Test
    @Category(TransferInterface.class)
    public void testGetEndpoint() throws GlobusClientException
    {
        Endpoint endpoint = client.getEndpoint(TEST_ENDPOINT);
        assertNotNull(endpoint);
        System.out.println("Got back endpoint: " + endpoint);
    }


    private String doDirectoryCreation() throws GlobusClientException
    {
        client.activateEndpoint(TEST_ENDPOINT);
        String dirName = randomFileName();
        boolean success = client.createDirectory(TEST_ENDPOINT, dirName);
        return success ? dirName : null;
    }


    /**
     * @return
     */
    private String randomFileName()
    {
        UUID randomUUID = UUID.randomUUID();
        String dirName = "/~/testCreation-" + randomUUID.toString();
        return dirName;
    }


    @Test
    @Category(TransferInterface.class)
    public void testCreateDirectory() throws GlobusClientException
    {
        assertNotNull(doDirectoryCreation());
    }


    private String doShareCreation() throws GlobusClientException
    {
        String dirName = doDirectoryCreation();
        String shareName = dirName.replace('/', 'I');
        shareName = shareName.replace('~', 'X');
        shareName = shareName.replace('-', 't');
        shareName = "ShareFor" + shareName;
        boolean success =
            client.setEndpointSharing(TEST_ENDPOINT, dirName, shareName,
                                      "Test share for Directory " + dirName);
        return success ? shareName : null;
    }


    private String doShareCreationAndPermissions() throws GlobusClientException
    {
        String shareName = doShareCreation();
        assertNotNull(shareName);

        client.createDirectory(shareName, TEST_TEMPDIR);

        boolean success =
            client.setEndpointAccessPermissions(TEST_USER + "#" + shareName, TEST_TEMPDIR,
                                                PrincipalType.user, TEST_SHARING_USER, "rw", false);
        assertTrue(success);
        return shareName;
    }


    @Test
    @Category(TransferInterface.class)
    public void testSetSharing() throws GlobusClientException
    {
        String shareName = doShareCreation();
        assertNotNull(shareName);
    }


    @Test
    @Category(TransferInterface.class)
    public void testSetSharePermissions() throws GlobusClientException
    {
        String shareName = doShareCreationAndPermissions();
        System.out.println("Endpoint " + TEST_USER + "#" + shareName
            + " is now writeable by pruyne");
    }


    @Test
    @Category(TransferInterface.class)
    public void testAccessList() throws GlobusClientException
    {
        String sharedEP = doShareCreationAndPermissions();
        TransferAccessList accessList = client.getAccessList(TEST_USER + "#" + sharedEP);
        assertNotNull(accessList);
        TransferAccess[] items = accessList.accessItems;
        assertNotNull(items);
        System.out.println("Access List: " + accessList);
        System.out.println("Access items:");
        for (TransferAccess item : items) {
            System.out.println(item);
        }
        client.deleteAccess(accessList.endpoint, items[0]);
    }


    @Test
    @Category(TransferInterface.class)
    public void testTransfer() throws GlobusClientException
    {
        String srcEp = "go#ep2";
        String destEp = "go#ep1";
        String srcFile = "/~/file1.txt";

        String destFile = "/~/file1_copy" + new Date().toString() + ".txt";

        client.activateEndpoint(srcEp);
        client.activateEndpoint(destEp);

        TransferJob tj = new TransferJob(client, srcEp, destEp);
        tj.addTransferItem(srcFile, destFile, false);
        Task task = tj.execute();
        assertNotNull(task);
        System.out.println("Got back task " + task);
        Task.Status pollStatus = task.pollStatus();
        System.out.println("Task status: " + pollStatus);
    }


    @Test
    @Category(TransferInterface.class)
    public void testTaskList() throws GlobusClientException
    {
        Task resultTask = new Task();
        resultTask.bytesTransferred = 0L;
        resultTask.completionTime = new Date();
        resultTask.destinationEndpoint = "";

        /*
        Task filterTask = new Task();
        filterTask.destinationEndpoint = "go#ep1";
        */
        TaskList taskList = client.taskList(null, resultTask, 0, 8);
        assertNotNull(taskList);
        assertTrue(taskList.length <= 8);
        System.out.println("Got back taskList: " + taskList);
    }

    
    @Test
    @Category(TransferInterface.class)
    public void testSuccessfulTransfers() throws GlobusClientException
    {
    	String transferId = "90a58156-debd-11e5-978b-22000b9da45e";
        SuccessfulTransfers sTransfers = client.successfulTransfers(transferId, null, 1);
        for (SuccessfulTransfer transfer : sTransfers.data) {
            System.out.println(transfer.destPath);
        }
        System.out.println("Got back successful transfers: " + sTransfers);
    }
    
    @Test
    @Category(TransferInterface.class)
    public void testDeleteDirectory() throws GlobusClientException
    {
        String dirName = doDirectoryCreation();
        boolean success = client.deletePath(TEST_ENDPOINT, dirName, true);
        assert(success);
    }
    
    @Test
    @Category(TransferInterface.class)
    public void testRename() throws GlobusClientException
    {
        String dirName = doDirectoryCreation();
        String newName = randomFileName();
        boolean success = client.rename(TEST_ENDPOINT, dirName, newName);
        assert(success);
    }

    private <T> T deserializeTest(String jsonResourceFile, Class<T> retClass) throws IOException 
    {
        String jsonRep = resourceFileToString(jsonResourceFile);
//        System.out.println("Json:" + jsonRep);
        return GlobusEntity.fromJson(jsonRep, retClass);
    }
    
    @Test
    @Category(GlobusEntity.class)
    public void testTokenDeserialize() throws IOException
    {
        GlobusAuthToken gat = deserializeTest("access_token.json", GlobusAuthToken.class); 
        assertNotNull(gat);
        System.out.println("Created token: " + gat);
    }
    
}
