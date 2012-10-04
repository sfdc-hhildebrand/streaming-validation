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

/**
 * @author hhildebrand
 *
 */
/*
* Copyright, 2010-2011, SALESFORCE.com
* All Rights Reserved
* Company Confidential
*/

import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * This example demonstrates how a streaming client works against Salesforce
 * Streaming API
 */
public class Debugger {

    private static final String DEFAULT_CONFIGURATION_FILE = "config.yml";

    public static void main(String[] argv) throws Exception {
        String configFile = DEFAULT_CONFIGURATION_FILE;
        if (argv.length > 1) {
            System.err.println("Invocation takes max of one argument, the path to the configuration file");
            System.exit(1);
        } else if (argv.length == 1) {
            configFile = argv[0];
        }
        new Debugger(Configuration.fromYaml(new FileInputStream(configFile))).run();
    }

    private static void waitForHandshake(BayeuxClient client,
                                         long timeoutInMilliseconds,
                                         long intervalInMilliseconds) {
        long start = System.currentTimeMillis();
        long end = start + timeoutInMilliseconds;
        while (System.currentTimeMillis() < end) {
            if (client.isHandshook()) {
                return;
            }
            try {
                Thread.sleep(intervalInMilliseconds);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("Client did not handshake with server");
    }

    private final Configuration configuration;

    public Debugger(Configuration configuration) {
        this.configuration = configuration;
    }

    public void run() throws Exception {
        if (!configuration.isDebug()) {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root")).setLevel(Level.INFO);
        }
        System.out.println("Running streaming client ...");

        final BayeuxClient client = makeClient();

        client.getChannel(Channel.META_HANDSHAKE).addListener(handshakeListener());
        client.getChannel(Channel.META_CONNECT).addListener(connectListener());
        client.getChannel(Channel.META_SUBSCRIBE).addListener(subscribeListener());

        client.handshake();
        System.out.println("Waiting for handshake");
        waitForHandshake(client, 30 * 1000, 1000);

        if (!client.isHandshook()) {
            System.out.println("Failed to handshake");
            System.exit(1);
        }

        if (configuration.getChannel() != null) {
            System.out.println("Subscribing for channel: "
                               + configuration.getChannel());
            client.getChannel(configuration.getChannel()).subscribe(messageListener());
        } else {
            System.out.println("No channel to subscribe to");
        }
        System.out.println("Waiting for streamed data from salesforce ...");
        while (true) {
            Thread.sleep(4000);
        }
    }

    private BayeuxClient makeClient() throws Exception {
        HttpClient httpClient = new HttpClient();
        httpClient.setConnectTimeout(configuration.getConnectTimeout());
        httpClient.setTimeout(configuration.getReadTimeout());
        httpClient.start();

        String[] pair = SoapLoginUtil.login(configuration, httpClient);
        if (pair == null) {
            System.exit(1);
        }
        assert pair.length == 2;
        String sessionid = pair[0];
        String endpoint = pair[1];
        System.out.println("Login successful!\nEndpoint: " + endpoint
                           + "\nSessionid=" + sessionid);

        Map<String, Object> options = new HashMap<String, Object>();
        options.put(ClientTransport.TIMEOUT_OPTION,
                    configuration.getReadTimeout());
        return new BayeuxClient(salesforceStreamingEndpoint(endpoint),
                                new AuthorizedLongPollingTransport(sessionid,
                                                                   options,
                                                                   httpClient));
    }

    private String salesforceStreamingEndpoint(String endpoint)
                                                               throws MalformedURLException {
        return new URL(endpoint + configuration.getStreamingUri()).toExternalForm();
    }

    /**
     * @return
     */
    protected MessageListener connectListener() {
        return new ClientSessionChannel.MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                System.out.println("[CHANNEL:META_CONNECT]: " + message);
                boolean success = message.isSuccessful();
                if (!success) {
                    String error = (String) message.get("error");
                    if (error != null) {
                        System.out.println("Error during CONNECT: " + error);
                        System.out.println("Exiting...");
                        System.exit(1);
                    }
                }
            }
        };
    }

    /**
     * @return
     */
    protected MessageListener handshakeListener() {
        return new ClientSessionChannel.MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                System.out.println("[CHANNEL:META_HANDSHAKE]: " + message);
                boolean success = message.isSuccessful();
                if (!success) {
                    String error = (String) message.get("error");
                    if (error != null) {
                        System.out.println("Error during HANDSHAKE: " + error);
                        System.out.println("Exiting...");
                        System.exit(1);
                    }
                }
            }
        };
    }

    /**
     * @return
     */
    protected MessageListener messageListener() {
        return new MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                System.out.println("Received Message: " + message);
            }
        };
    }

    /**
     * @return
     */
    protected MessageListener subscribeListener() {
        return new ClientSessionChannel.MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                System.out.println("[CHANNEL:META_SUBSCRIBE]: " + message);
                boolean success = message.isSuccessful();
                if (!success) {
                    String error = (String) message.get("error");
                    if (error != null) {
                        System.out.println("Error during SUBSCRIBE: " + error);
                        System.out.println("Exiting...");
                        System.exit(1);
                    }
                }
            }
        };
    }
}
