/*
 * Copyright 2014 University of Chicago
 * All rights reserved.
 */

package org.globus.transfer;

import org.globus.GlobusClientException;
import org.globus.GlobusRestInterface;

/**
 * More documentation on the Transfer interface can be found at: 
 * <a href="https://github.com/globusonline/koa/tree/master/doc/manual">Transfer API Manual</a>
 * @author pruyne
 *
 */
public interface TransferInterface extends GlobusRestInterface
{
    enum PrincipalType {
        user,
        identity,
        group,
        anonymous,
        all_authenticated_users;
    }


    public boolean activateEndpoint(String endPoint) throws GlobusClientException;


    public TransferDirectoryListing lsDirectory(String endPoint, String directory)
        throws GlobusClientException;


    public Endpoint getEndpoint(String endpointName) throws GlobusClientException;


    /**
     * Create a directory on a specified endpoint.
     * @param endpointName The endpoint on which to create the directory
     * @param directoryName The name of the new directory. Per {@link https
     *            ://transfer.api.globusonline.org /v0.10/doc/index.html#creating-directories} the
     *            directory is relative to the user's home directory if an absolute path is not
     *            given.
     * @return {@code true} on success
     * @throws GlobusClientException If directory creation fails.
     * @see https://transfer.api.globusonline.org/v0.10/doc/index.html#creating-directories
     */
    public boolean createDirectory(String endpointName, String directoryName)
        throws GlobusClientException;


    public boolean setEndpointSharing(String endpoint, String sharePath, String sharingName,
                                      String sharingComment) throws GlobusClientException;


    /**
     * TODO:
     * @param endpointName
     * @param path
     * @param principalType
     * @param principalName
     * @param principalPerms
     * @param editable
     * @return
     * @throws GlobusClientException
     */
    public boolean setEndpointAccessPermissions(String endpointName, String path,
                                                PrincipalType principalType, String principalName,
                                                String principalPerms, boolean editable)
                                                    throws GlobusClientException;


    /**
     * TODO:
     * @param endpointName
     * @return
     * @throws GlobusClientException
     */
    public TransferAccessList getAccessList(String endpointName) throws GlobusClientException;


    /**
     * TODO:
     * @param endpointName
     * @param accessItem
     * @throws GlobusClientException
     */
    public void deleteAccess(String endpointName, TransferAccess accessItem)
        throws GlobusClientException;


    /**
     * TODO:
     * @return
     * @throws GlobusClientException
     */
    public TaskList taskList() throws GlobusClientException;


    /**
     * Create a submission id value which may be used as an id in subsequent invocations of transfer
     * jobs. Note that this library typically creates these ids on behalf of the user, so use of
     * this method directly is likely to be uncommon.
     * @return A submission id string (UUID) which can be used in subsequent calls to create or
     *         maintain long running transfer jobs.
     * @throws GlobusClientException In the event of failure between the client and the Globus
     *             service.
     */
    public String createSubmissionId() throws GlobusClientException;


    /**
     * Return a list of tasks for the current user. The set of tasks returned can be setting
     * appropriate values in the parameter values.
     * @param filterVals If this is {@code non-null}, any fields with {@code non-null} values will
     *            be treated as filters. That is, only tasks with values matching the value set will
     *            be returned.
     * @param resultFields If this is {@code non-null}, only fields with {@code non-null} values
     *            will be set on the tasks returned in the {@link TaskList}.
     * @param offset If this is {@code non-null} the tasks returned will be starting at offset in
     *            the overall list of tasks. If the value is {@code null}, the default value of 0
     *            will be used.
     * @param limit If this is {@code non-null} only this many tasks will be returned in the list.
     *            If the value is null, the Globus Transfer default value of 10 will be used.
     * @return A {@link TaskList} object containing the values of the tasks returned subject to the
     *         constraints set in the parameters.
     * @throws GlobusClientException If any failures occur in communication with the Globus Transfer
     *             Service.
     */
    public TaskList taskList(Task filterVals, Task resultFields, Integer offset, Integer limit)
        throws GlobusClientException;


    /**
     * Return a list of successful transfer for the current user.
     * @param transferId Only succesful transfers with this id will be returned.
     * @param offset If this is {@code non-null} the tasks returned will be starting at offset in
     *            the overall list of tasks. If the value is {@code null}, the default value of 0
     *            will be used.
     * @param limit If this is {@code non-null} only this many tasks will be returned in the list.
     *            If the value is null, the Globus Transfer default value of 10 will be used.
     * @throws GlobusClientException If any failures occur in communication with the Globus Transfer
     *             Service.
     */
    public SuccessfulTransfers successfulTransfers(String transferId, Integer offset, Integer limit)
        throws GlobusClientException;


    /**
     * Delete a file, directory or sub-tree from a destination endpoint. This is a convenience
     * method on top of {@link deletePathAsync} for the common case of synchronous operation and no
     * notifications sent on completion of the delete.
     * @param endPoint The endpoint on which the deleted tree resides
     * @param deletePath The path to the directory or file to be deleted
     * @param recursive Whether to recurse and delete all files and sub-directories beneath the
     *            provided pat to be deleted
     * @return {@code true} on success, {@code false} on a catchable error
     * @throws GlobusClientException If any failure occurs performing in the operation on the Globus
     *             Transfer service.
     */
    public boolean deletePath(String endPoint, String deletePath, boolean recursive)
        throws GlobusClientException;


    /**
     * Delete a file, directory or sub-tree from a destination endpoint.
     * @param endPoint The endpoint on which the deleted tree resides
     * @param deletePath The path to the directory or file to be deleted
     * @param recursive Whether to recurse and delete all files and sub-directories beneath the
     *            provided pat to be deleted
     * @param notifyOnSuccess Send notification messages when the operation completes successfully.
     * @param notifyOnFailure Send notification messages if the operation fails.
     * @return A {@link Task} which can be used to monitor the status of the delete operation.
     * @throws GlobusClientException If any failure occurs performing in the operation on the Globus
     *             Transfer service.
     */
    public Task deletePathAsync(String endPoint, String deletePath, boolean recursive,
                                boolean notifyOnSuccess, boolean notifyOnFailure)
                                    throws GlobusClientException;


    /**
     * Rename or move a file or directory.
     * @param endPoint The endpoint on which the source file or directory resides.
     * @param srcPath The path to the source file which will be renamed or moved.
     * @param destPath The new name or path for the file.
     * @return {@code true} if the rename was successful. {@code false} if it failed for any reason
     *         that does cause an exception.
     * @throws GlobusClientException If any failure occurs performing the operation on the Globus
     *             Transfer service.
     */
    public boolean rename(String endPoint, String srcPath, String destPath)
        throws GlobusClientException;

}