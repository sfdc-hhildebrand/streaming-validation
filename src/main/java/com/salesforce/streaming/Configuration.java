/**
 * Copyright (c) 2012, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.streaming;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * @author hhildebrand
 * 
 */
public class Configuration {
    private static final int    CONNECT_TIMEOUT           = 20 * 1000;
    private static final int    READ_TIMEOUT              = 120 * 1000;
    private static final String SERVICES_SOAP_PARTNER_URI = "/services/Soap/u/22.0/";
    private static final String STREAMING_ENDPOINT_URI    = "/cometd/23.0";
    static final String         LOGIN_ENDPOINT            = "https://login.salesforce.com";

    public static Configuration fromYaml(InputStream yaml)
                                                          throws JsonParseException,
                                                          JsonMappingException,
                                                          IOException {
        Configuration configuration = new ObjectMapper(new YAMLFactory()).readValue(yaml,
                                                                                    Configuration.class);
        yaml.close();
        return configuration;
    }

    @JsonProperty
    private String  channel;
    @JsonProperty
    private int     connectTimeout = CONNECT_TIMEOUT;
    @JsonProperty
    private String  loginEndpoint  = LOGIN_ENDPOINT;
    @JsonProperty
    private String  password;
    @JsonProperty
    private int     readTimeout    = READ_TIMEOUT;
    @JsonProperty
    private String  soapPartnerUri = SERVICES_SOAP_PARTNER_URI;
    @JsonProperty
    private String  streamingUri   = STREAMING_ENDPOINT_URI;
    @JsonProperty
    private String  username;
    @JsonProperty
    private boolean debug          = false;

    public boolean isDebug() {
        return debug;
    }

    public String getChannel() {
        return channel;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    public String getPassword() {
        return password;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public String getSoapPartnerUri() {
        return soapPartnerUri;
    }

    public String getStreamingUri() {
        return streamingUri;
    }

    public String getUsername() {
        return username;
    }
}
