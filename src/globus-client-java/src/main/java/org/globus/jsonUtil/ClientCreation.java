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
package org.globus.jsonUtil;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/**
 * @author pruyne
 *
 */
public class ClientCreation
{

    public static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] certs, String authType)
        {
            return;
        }


        public void checkServerTrusted(X509Certificate[] certs, String authType)
        {
            return;
        }


        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    } };
    public static HostnameVerifier allHostsValid = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session)
        {
            return true;
        }
    };


    /**
     *
     */
    public static Client createRestClient()
    {
        ClientConfig config = new DefaultClientConfig();
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAllCerts, null);
            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                                       new HTTPSProperties(allHostsValid, ctx));
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client restClient = Client.create(config);
        return restClient;
    }


    public static Client createRestClient(File accessTokenFile) throws IOException
    {
        GlobusAuthFilter authFilter = new GlobusAuthFilter(accessTokenFile);
        Client restClient = createRestClient();
        restClient.addFilter(authFilter);
        return restClient;
    }
}
