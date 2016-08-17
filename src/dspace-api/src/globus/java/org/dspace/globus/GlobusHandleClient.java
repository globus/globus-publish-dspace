package org.dspace.globus;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;

import org.apache.log4j.Logger;

/**
 * This class represents authentication to the handle system for the purpose of
 * creating handles with a specific prefix.
 *
 *
 */
public class GlobusHandleClient
{
    private static final Logger logger = Logger
            .getLogger(GlobusHandleClient.class);

    public enum HandleDataType {
        HS_ADMIN, HS_SECKEY, EMAIL, URL, HS_PUBKEY, URN, HS_SERV, HS_VLIST, HS_ALIAS, INET_HOST, HS_STRING;

        public byte[] asBytes()
        {
            try
            {
                return this.toString().getBytes("UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                logger.error("Unable to encode string " + this.toString()
                        + " in UTF-8", e);
                return null;
            }
        }
    }

    private static HandleResolver resolver;

    PublicKeyAuthenticationInfo pubKeyAuthInfo;

    private byte[] authHandleBytes;

    private int authIndex;

    private AdminRecord admin;

    private String prefix;

    public GlobusHandleClient(File keyFile, String authorizationHandle,
            int authIndex, String keyPasscode, String prefix) throws Exception
    {
        this(bytesForFile(keyFile), authorizationHandle, authIndex, keyPasscode, prefix);
    }

    /**
     * Create a client for accessing the Handle system.
     *
     * @param keyBytes
     * @param authorizationHandle
     * @param authIndex
     * @param keyPasscode
     * @param prefix
     * @throws Exception
     */
    public GlobusHandleClient(byte[] keyBytes, String authorizationHandle,
            int authIndex, String keyPasscode, String prefix) throws Exception
    {
        PrivateKey privKey = keyFromBytes(keyBytes, keyPasscode);
        // 300 seems to be the default value for the authIndex
        if (authIndex < 0)
        {
            authIndex = 300;
        }
        this.authIndex = authIndex;

        this.prefix = prefix;

        // If no explicit authorizationHandle is given, but we do have the
        // prefix, we'll
        // assume the default value
        if (authorizationHandle == null && prefix != null)
        {
            authorizationHandle = "0.NA/" + prefix;
        }
        authHandleBytes = authorizationHandle.getBytes("UTF8");
        pubKeyAuthInfo = new PublicKeyAuthenticationInfo(authHandleBytes,
                authIndex, privKey);
        // We don't want to create a handle without an admin value--
        // otherwise
        // we would be locked out. Give ourselves all permissions, even
        // ones that only apply for NA handles.
        admin = new AdminRecord(authHandleBytes, authIndex, true, true, true,
                true, true, true, true, true, true, true, true, true);

        // Init some constants and statics we'll want later
        synchronized (GlobusHandleClient.class)
        {
            if (resolver == null)
            {
                resolver = new HandleResolver();
            }
        }
    }

    /**
     * @param keyFile
     * @param keyPasscode
     * @return
     * @throws Exception
     */
    private PrivateKey keyFromBytes(byte[] keyBuf, String keyPasscode)
            throws Exception
    {
        byte[] passcodeBytes = null;
        if (keyPasscode != null)
        {
            passcodeBytes = keyPasscode.getBytes();
        }
        keyBuf = Util.decrypt(keyBuf, passcodeBytes);
        PrivateKey privKey = Util.getPrivateKeyFromBytes(keyBuf, 0);
        return privKey;
    }

    /**
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static byte[] bytesForFile(File file) throws FileNotFoundException,
            IOException
    {
        FileInputStream fis = new FileInputStream(file);
        long keySize = file.length();
        byte[] keyBuf = new byte[(int) keySize];
        fis.read(keyBuf);
        fis.close();
        return keyBuf;
    }

    private HandleValue createAdminRecordHandleValue()
    {
        // All handle values need a timestamp, so get the current time in
        // seconds since the epoch
        int timestamp = (int) (System.currentTimeMillis() / 1000);

        // Now build the HandleValue object for our admin value. The first
        // argument is the value's index, 100 in this case. The second
        // argument is the value's type. The third argument holds the value's
        // data. Since this is binary data, not textual data like a URL, we have
        // to encode it first.
        //
        // The other arguments can usually just be copied verbatim from here.
        // The fourth argument indicates whether the time to live for the
        // value is absolute or relative. The fifth argument is the time to
        // live, 86400 seconds(24 hours) in this case. The sixth argument is
        // the timestamp we created earlier. The seventh argument is a
        // ValueReference array. You will almost always want to leave this
        // null; read the RFC's for more information. The last four arguments
        // are the permissions for the value: admin read, admin write, public
        // read, and public write.
        //
        // whew!
        HandleValue adminHandleVal = new HandleValue(100,
                HandleDataType.HS_ADMIN.asBytes(),
                Encoder.encodeAdminRecord(admin),
                HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true,
                true, true, false);
        return adminHandleVal;
    }

    public static HandleValue createHandleValue(int index, URL url)
    {
        return createHandleValue(index, HandleDataType.URL, url);
    }

    public static HandleValue createHandleValue(int index, HandleDataType type,
            Object value)
    {
        if (type == null || value == null)
        {
            return null;
        }
        byte[] handleType = type.asBytes();
        byte[] valBytes;
        HandleValue hv = null;
        try
        {
            valBytes = value.toString().getBytes("UTF-8");
            hv = new HandleValue(index, handleType, valBytes);
        }
        catch (UnsupportedEncodingException e)
        {
            // No sane jdk will get here...
        }

        return hv;
    }

    /**
     * Create a new handle with a single URL value (stored at index 1)
     *
     * @param handleId
     *            The ide of the Handle
     * @param url
     *            The URL to be stored in the handle.
     * @return {@code true} on success
     * @throws HandleException
     *             on unexpected failure
     * @see GlobusHandleClient#createHandle(String, Collection)
     */
    public boolean createHandle(String handleId, URL url)
            throws HandleException
    {
        HandleValue urlHandleValue = createHandleValue(1, url);
        return createHandle(handleId, Arrays.asList(urlHandleValue));
    }

    /**
     * Create a new handle in the handle server containing all values in the
     * values collection. The prefix will be the prefix associated with this
     * client.
     *
     * @param handleId
     *            The id of the handle to be created. This is the portion after
     *            the prefix and the /
     * @param values
     *            The set of values to be included in the handle.
     * @return {@code true} on success, or {@code false} if the creation fails
     *         in a non-catastrophic manner (e.g. Detecting an existing handle
     *         with the handle id).
     * @throws HandleException
     *             If creation of the handle fails unexpectedly.
     */
    public boolean createHandle(String handleId, Collection<HandleValue> values)
            throws HandleException
    {
        if (handleId == null)
        {
            return false;
        }

        ArrayList<HandleValue> valList = new ArrayList<HandleValue>(values);
        valList.add(createAdminRecordHandleValue());
        if (prefix != null && !handleId.startsWith(prefix))
        {
            logger.info("Prepending prefix " + prefix + " to input handle id "
                    + handleId);
            handleId = prefix + "/" + handleId;
        }

        byte[] handleIdBytes;
        try
        {
            handleIdBytes = handleId.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return false;
        }
        CreateHandleRequest createReq = new CreateHandleRequest(handleIdBytes,
                valList.toArray(new HandleValue[0]), pubKeyAuthInfo);

        AbstractResponse response = resolver.processRequest(createReq);

        // The responseCode value for a response indicates the status of
        // the request. A successful resolution will always return
        // RC_SUCCESS. Failed resolutions could return one of several
        // response codes, including RC_ERROR, RC_INVALID_ADMIN, and
        // RC_INSUFFICIENT_PERMISSIONS.
        if (response.responseCode == AbstractMessage.RC_SUCCESS)
        {
            return true;
        }
        else
        {
            logger.warn("Creation of handle "
                    + handleId
                    + " failed with response "
                    + AbstractResponse
                            .getResponseCodeMessage(response.responseCode));
            return false;
        }
    }

    public static void main(String[] args)
    {
        String keyFile;
        String keyPasscode;
        String prefix;
        String handleName;
        if (args.length < 4)
        {
            System.err
                    .println("Usage: <key file name> <passcode> <handle prefix> [{-string|-url} value]*");
        }
        keyFile = args[0];
        keyPasscode = args[1];
        prefix = args[2];
        handleName = args[3];
        handleName = handleName.replace("%t",
                String.valueOf(System.currentTimeMillis()));
        List<HandleValue> vals = new ArrayList<HandleValue>();
        for (int i = 4; i < args.length; i++)
        {
            String type = args[i];
            byte[] value = null;
            if (type.startsWith("-"))
            {
                type = type.substring(1);
                i++; // Increment index
                byte[] handleType = null;
                if (i < args.length)
                {
                    value = args[i].getBytes();
                }
                if ("url".equalsIgnoreCase(type))
                {
                    handleType = HandleDataType.URL.asBytes();
                }
                else if ("string".equalsIgnoreCase(type))
                {
                    handleType = HandleDataType.HS_STRING.asBytes();
                }
                else
                {
                    System.err.println("Unknown option -" + type);
                }
                if (handleType != null && value != null)
                {
                    HandleValue hv = new HandleValue(vals.size() + 1,
                            handleType, value);
                    vals.add(hv);
                }
            }
        }
        if (vals.size() > 0)
        {
            GlobusHandleClient ghc = null;
            try
            {
                ghc = new GlobusHandleClient(new File(keyFile), null, -1,
                        keyPasscode, prefix);
                ghc.createHandle(handleName, vals);
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
            }
        }
    }
}
