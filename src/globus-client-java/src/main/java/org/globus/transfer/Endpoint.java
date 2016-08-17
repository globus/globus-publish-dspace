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
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author pruyne
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Endpoint extends TransferEntity
{
    @JsonProperty("id")
    public String id;

    @JsonProperty("display_name")
    public String displayName;

    @JsonProperty("organization")
    public String organization;

    @JsonProperty("department")
    public String department;

    @JsonProperty("keywords")
    public String keywords;

    @JsonProperty("contact_email")
    public String contactEmail;

    @JsonProperty("contact_info")
    public String contactInfo;

    @JsonProperty("info_link")
    public String infoLink;

    @JsonProperty("subscription_id")
    public String subscriptionId;

    @JsonProperty("gcp_connected")
    public Boolean gcpConnected;

    @JsonProperty("gcp_paused")
    public Boolean gcpPaused;

    @JsonProperty("network_use")
    public String networkUse;

    @JsonProperty("location")
    public String location;

    @JsonProperty("min_concurrency")
    public Integer minConcurrency;

    @JsonProperty("preferred_concurrency")
    public Integer preferredConcurrency;

    @JsonProperty("max_concurrency")
    public Integer maxConcurrency;

    @JsonProperty("min_parallelism")
    public Integer minParallelism;

    @JsonProperty("preferred_parallelism")
    public Integer preferredParallelism;

    @JsonProperty("max_parallelism")
    public Integer maxParallelism;

    @JsonProperty("owner_id")
    public String ownerId;

    @JsonProperty("owner_string")
    public String ownerString;

    @JsonProperty("sharing_target_root_path")
    public String sharingTargetRootPath;

    @JsonProperty("activated")
    public Boolean activated;

    @JsonProperty("disable_verify")
    public Boolean disableVerify;

    @JsonProperty("myproxy_server")
    public String myproxyServer;

    @JsonProperty("DATA")
    public TransferServerGen[] servers;

    static class ClassForlsLink
    {
        @JsonProperty("href")
        public String href;

        @JsonProperty("resource")
        public String resource;

        @JsonProperty("rel")
        public String rel;

        @JsonProperty("title")
        public String title;

    }
    @JsonProperty("ls_link")
    public ClassForlsLink lsLink;

    @JsonProperty("expires_in")
    public Long expiresIn;

    @Deprecated
    @JsonProperty("canonical_name")
    public String canonicalName;

    @JsonProperty("sharing_target_endpoint")
    public String sharingTargetEndpoint;

    @JsonProperty("acl_available")
    public Boolean aclAvailable;

    @JsonProperty("s3_url")
    public String s3Url;

    @JsonProperty("public")
    public Boolean pub; // public is a Java keyword

    @JsonProperty("default_directory")
    public String defaultDirectory;

    @JsonProperty("username")
    public String username;

    @JsonProperty("globus_connect_setup_key")
    public String globusConnectSetupKey;

    @JsonProperty("description")
    public String description;

    @JsonProperty("in_use")
    public Boolean inUse;

    @JsonProperty("force_encryption")
    public Boolean forceEncryption;

    @JsonProperty("myproxy_dn")
    public String myproxyDn;

    @JsonProperty("expire_time")
    public String expireTime;

    @Deprecated
    @JsonProperty("acl_editable")
    public boolean aclEditable;

    @JsonProperty("oauth_server")
    public String oauthServer;

    @Deprecated
    @JsonProperty("host_endpoint")
    public String hostEndpoint;

    @JsonProperty("host_endpoint_id")
    public String hostEndpointId;

    @JsonProperty("host_endpoint_display_name")
    public String hostEndpointDisplayname;

    @JsonProperty("name")
    public String name;

    @JsonProperty("is_globus_connect")
    public Boolean isGlobusConnect;

    @JsonProperty("is_go_storage")
    public Boolean isGoStorage;

    @JsonProperty("s3_owner_activated")
    public Boolean s3OwnerActivated;

    @JsonProperty("shareable")
    public Boolean shareable;

    @JsonProperty("host_path")
    public String hostPath;

    @JsonProperty("https_server")
    public String httpsServer;

    @JsonProperty("my_effective_roles")
    public String[] myEffectiveRoles;
}
