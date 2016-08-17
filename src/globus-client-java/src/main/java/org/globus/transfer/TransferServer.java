/*
 * Copyright 2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.globus.transfer;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author pruyne
 *
 */
public class TransferServer extends TransferEntity
{

    @JsonProperty("is_paused")
    boolean isPaused;

    @JsonProperty("hostname")
    String hostname;

    @JsonProperty("uri")
    String uri;

    @JsonProperty("port")
    int port;

    @JsonProperty("scheme")
    String scheme;

    @JsonProperty("is_connected")
    boolean isConnected;

    @JsonProperty("id")
    long id;

    @JsonProperty("subject")
    String subject;


    /**
     * @return the isPaused
     */
    public boolean isPaused()
    {
        return isPaused;
    }


    /**
     * @param isPaused the isPaused to set
     */
    public void setPaused(boolean isPaused)
    {
        this.isPaused = isPaused;
    }


    /**
     * @return the hostname
     */
    public String getHostname()
    {
        return hostname;
    }


    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }


    /**
     * @return the uri
     */
    public String getUri()
    {
        return uri;
    }


    /**
     * @param uri the uri to set
     */
    public void setUri(String uri)
    {
        this.uri = uri;
    }


    /**
     * @return the port
     */
    public int getPort()
    {
        return port;
    }


    /**
     * @param port the port to set
     */
    public void setPort(int port)
    {
        this.port = port;
    }


    /**
     * @return the scheme
     */
    public String getScheme()
    {
        return scheme;
    }


    /**
     * @param scheme the scheme to set
     */
    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }


    /**
     * @return the isConnected
     */
    public boolean isConnected()
    {
        return isConnected;
    }


    /**
     * @param isConnected the isConnected to set
     */
    public void setConnected(boolean isConnected)
    {
        this.isConnected = isConnected;
    }


    /**
     * @return the id
     */
    public long getId()
    {
        return id;
    }


    /**
     * @param id the id to set
     */
    public void setId(long id)
    {
        this.id = id;
    }


    /**
     * @return the subject
     */
    public String getSubject()
    {
        return subject;
    }


    /**
     * @param subject the subject to set
     */
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

}
