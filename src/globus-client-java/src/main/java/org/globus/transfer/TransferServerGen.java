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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class TransferServerGen extends TransferEntity
{
    @JsonProperty("is_paused")
    public Boolean isPaused;

    @JsonProperty("hostname")
    public String hostname;

    @JsonProperty("uri")
    public String uri;

    @JsonProperty("port")
    public Integer port;

    @JsonProperty("scheme")
    public String scheme;

    @JsonProperty("is_connected")
    public Boolean isConnected;

    @JsonProperty("id")
    public Integer id;

    @JsonProperty("subject")
    public String subject;

}
