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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author hhildebrand
 * 
 */
public final class SoapLoginUtil {

    private static final String ENV_START = "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' "
                                            + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                                            + "xmlns:urn='urn:partner.soap.sforce.com'><soapenv:Body>";

    private static final String ENV_END   = "</soapenv:Body></soapenv:Envelope>";

    private static byte[] soapXmlForLogin(String username, String password)
                                                                           throws UnsupportedEncodingException {
        return (ENV_START + "  <urn:login>" + "    <urn:username>" + username
                + "</urn:username>" + "    <urn:password>" + password
                + "</urn:password>" + "  </urn:login>" + ENV_END).getBytes("UTF-8");
    }

    public static String[] login(Configuration configuration, HttpClient client)
                                                                                throws IOException,
                                                                                InterruptedException,
                                                                                SAXException,
                                                                                ParserConfigurationException {
        String soapURL = new URL(new URL(configuration.getLoginEndpoint())
                                 + configuration.getSoapPartnerUri()).toExternalForm();
        ContentExchange exchange = new ContentExchange();
        exchange.setMethod("POST");
        exchange.setURL(soapURL);
        exchange.setRequestContentSource(new ByteArrayInputStream(
                                                                  soapXmlForLogin(configuration.getUsername(),
                                                                                  configuration.getPassword())));
        exchange.setRequestHeader("Content-Type", "text/xml");
        exchange.setRequestHeader("SOAPAction", "''");
        exchange.setRequestHeader("PrettyPrint", "Yes");
        new ExchangeListener(exchange);

        client.send(exchange);
        exchange.waitForDone();
        String response = exchange.getResponseContent();

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();

        LoginResponseParser parser = new LoginResponseParser();
        saxParser.parse(new ByteArrayInputStream(response.getBytes("UTF-8")),
                        parser);

        if (parser.sessionId == null || parser.serverUrl == null) {
            System.out.println("Login Failed for username=["
                               + configuration.getUsername() + "] SOAP-URL: ["
                               + soapURL + "]\n" + response);
            return null;
        }

        URL soapEndpoint = new URL(parser.serverUrl);
        StringBuilder endpoint = new StringBuilder().append(soapEndpoint.getProtocol()).append("://").append(soapEndpoint.getHost());
        if (soapEndpoint.getPort() > 0)
            endpoint.append(":").append(soapEndpoint.getPort());
        return new String[] { parser.sessionId, endpoint.toString() };
    }

    private static class LoginResponseParser extends DefaultHandler {

        private boolean inSessionId;
        private String  sessionId;

        private boolean inServerUrl;
        private String  serverUrl;

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inSessionId)
                sessionId = new String(ch, start, length);
            if (inServerUrl)
                serverUrl = new String(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName != null) {
                if (localName.equals("sessionId")) {
                    inSessionId = false;
                }
                if (localName.equals("serverUrl")) {
                    inServerUrl = false;
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {
            if (localName != null) {
                if (localName.equals("sessionId")) {
                    inSessionId = true;
                }
                if (localName.equals("serverUrl")) {
                    inServerUrl = true;
                }
            }
        }
    }
}
