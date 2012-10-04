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

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpEventListener;
import org.eclipse.jetty.io.Buffer;

/**
 * @author hhildebrand
 * 
 */
public class ExchangeListener implements HttpEventListener {
    private final ContentExchange   exchange;
    private final HttpEventListener previous;

    /**
     * @param exchange
     */
    public ExchangeListener(ContentExchange exchange) {
        this.exchange = exchange;
        previous = exchange.getEventListener();
        exchange.setEventListener(this);
    }

    @Override
    public void onConnectionFailed(Throwable ex) {
        if (previous != null) {
            previous.onConnectionFailed(ex);
        }
        System.out.println(String.format("Connection failed: %s",
                                         exchange.getRequestURI()));
    }

    @Override
    public void onException(Throwable ex) {
        if (previous != null) {
            previous.onException(ex);
        }
        System.out.println(String.format("Exception during request: %s",
                                         exchange.getRequestURI()));
        ex.printStackTrace();
    }

    @Override
    public void onExpire() {
        if (previous != null) {
            previous.onExpire();
        }
        System.out.println(String.format("Request: %s expired",
                                         exchange.getRequestURI()));
    }

    @Override
    public void onRequestCommitted() throws IOException {
        if (previous != null) {
            previous.onRequestCommitted();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Sending request: ").append(exchange.getRequestURI()).append('\n');
        builder.append(exchange.getRequestFields()).append('\n');
        System.out.println(builder.toString());
    }

    @Override
    public void onRequestComplete() throws IOException {
        if (previous != null) {
            previous.onRequestComplete();
        }
    }

    @Override
    public void onResponseComplete() throws IOException {
        if (previous != null) {
            previous.onResponseComplete();
        }
    }

    @Override
    public void onResponseContent(Buffer content) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Receiving response: ").append(exchange.getRequestURI()).append('\n');
        builder.append(exchange.getResponseFields()).append('\n');
        System.out.println(builder.toString());
        if (previous != null) {
            previous.onResponseContent(content);
        }
    }

    @Override
    public void onResponseHeader(Buffer name, Buffer value) throws IOException {
        if (previous != null) {
            previous.onResponseHeader(name, value);
        }
    }

    @Override
    public void onResponseHeaderComplete() throws IOException {
        if (previous != null) {
            previous.onResponseHeaderComplete();
        }
    }

    @Override
    public void onResponseStatus(Buffer version, int status, Buffer reason)
                                                                           throws IOException {
        if (previous != null) {
            previous.onResponseStatus(version, status, reason);
        }
        System.out.println(String.format("Response status: %s for: %s", status,
                                         exchange.getRequestURI()));
    }

    @Override
    public void onRetry() {
        if (previous != null) {
            previous.onRetry();
        }
        System.out.println(String.format("Retrying request: %s",
                                         exchange.getRequestURI()));
    }
}
