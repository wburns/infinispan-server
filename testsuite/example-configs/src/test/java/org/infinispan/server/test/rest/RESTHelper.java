/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.server.test.rest;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.*;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

import static junit.framework.Assert.assertEquals;

/**
 * Utility class.
 * 
 * @author <a href="mailto:mlinhard@redhat.com">Michal Linhard</a>
 * @author <a href="mailto:mgencur@redhat.com">Martin Gencur</a>
 */
public class RESTHelper {

    public static final String KEY_A = "a";
    public static final String KEY_B = "b";
    public static final String KEY_C = "c";

    private static final String DATE_PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    private static int port = 8080;
    private static List<Server> servers = new ArrayList<Server>();
    public static DefaultHttpClient client = new DefaultHttpClient();

    public static void addServer(String hostname, String restServerPath) {
        servers.add(new Server(hostname, restServerPath));
    }
    
    public static void addServer(String hostname, String port, String restServerPath) {
        RESTHelper.port = Integer.parseInt(port);
        servers.add(new Server(hostname, restServerPath));
    }

    public static String addDay(String aDate, int days) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN_RFC1123, Locale.US);
        Date date = format.parse(aDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return format.format(cal.getTime());
    }

    public static HttpResponse head(String uri) throws Exception {
        return head(uri, HttpServletResponse.SC_OK);
    }

    public static HttpResponse headWithoutClose(String uri) throws Exception {
        return head(uri, HttpServletResponse.SC_OK);
    }
    
    public static HttpResponse head(String uri, int expectedCode) throws Exception {
        return head(uri, expectedCode, new String[0][0]);
    }
    
    public static HttpResponse headWithoutClose(String uri, int expectedCode) throws Exception {
        return head(uri, expectedCode, new String[0][0]);
    }

    public static HttpResponse headWithout(String uri, int expectedCode, String[][] headers) throws Exception {
        HttpHead head = new HttpHead(uri);
        for (String[] eachHeader : headers) {
            head.setHeader(eachHeader[0], eachHeader[1]);
        }
        HttpResponse resp = client.execute(head);
        assertEquals(expectedCode, resp.getStatusLine().getStatusCode());
        return resp;
    }
    
    public static HttpResponse head(String uri, int expectedCode, String[][] headers) throws Exception {
        HttpHead head = new HttpHead(uri);
        HttpResponse resp = null;
        try {
            for (String[] eachHeader : headers) {
                head.setHeader(eachHeader[0], eachHeader[1]);
            }
            resp = client.execute(head);
        } finally {
            EntityUtils.consume(resp.getEntity());
        }
        assertEquals(expectedCode, resp.getStatusLine().getStatusCode());
        return resp;
    }
    
    public static HttpResponse get(String uri) throws Exception {
        return get(uri, HttpServletResponse.SC_OK);
    }
    
    public static HttpResponse getWithoutClose(String uri) throws Exception {
        return getWithoutClose(uri, HttpServletResponse.SC_OK);
    }

    public static HttpResponse get(String uri, String expectedResponseBody) throws Exception {
        return get(uri, expectedResponseBody, HttpServletResponse.SC_OK, true);
    }
    
    public static HttpResponse getWithoutClose(String uri, String expectedResponseBody) throws Exception {
        return get(uri, expectedResponseBody, HttpServletResponse.SC_OK, false);
    }
    
    public static HttpResponse get(String uri, int expectedCode) throws Exception {
        return get(uri, null, expectedCode, true, new Object[0]);
    }
    
    public static HttpResponse getWithoutClose(String uri, int expectedCode) throws Exception {
        return get(uri, null, expectedCode, false, new Object[0]);
    }
    
    public static HttpResponse get(String uri, String expectedResponseBody, int expectedCode, boolean closeConnection, Object... headers) throws Exception {
        HttpGet get = new HttpGet(uri);
        if (headers.length % 2 != 0)
            throw new IllegalArgumentException("bad headers argument");
        for (int i = 0; i < headers.length; i += 2) {
            get.setHeader((String) headers[i], (String) headers[i + 1]);
        }
        HttpResponse resp = client.execute(get);
        try {
            assertEquals(expectedCode, resp.getStatusLine().getStatusCode());
            if (expectedResponseBody != null) {
                StringBuilder responseBuilder = new StringBuilder();
                BufferedReader responseReader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
                String line = null;
                while ((line = responseReader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                assertEquals(expectedResponseBody, responseBuilder.toString());
            }
        }
        finally {
            if (closeConnection) {
                EntityUtils.consume(resp.getEntity());
            }
        }
        return resp;
    }

    public static HttpResponse put(String uri, Object data, String contentType) throws Exception {
        return put(uri, data, contentType, HttpServletResponse.SC_OK);
    }

    public static HttpResponse put(String uri, Object data, String contentType, int expectedCode) throws Exception {
        return put(uri, data, contentType, expectedCode, new Object[0]);
    }

    public static HttpResponse put(String uri, Object data, String contentType, int expectedCode, Object... headers)
            throws Exception {
        HttpPut put = new HttpPut(uri);
        if (data instanceof String) {
            put.setEntity(new StringEntity((String) data, contentType, "UTF-8"));
        } else if (data instanceof byte[]) {
            byte[] byteData = (byte[]) data;
            ByteArrayInputStream bs = new ByteArrayInputStream(byteData);
            put.setEntity(new InputStreamEntity(bs, byteData.length));
        } else {
            throw new IllegalArgumentException("Unknown data type for PUT method");
        }
        put.setHeader("Content-Type", contentType);
        if (headers.length % 2 != 0)
            throw new IllegalArgumentException("bad headers argument");
        for (int i = 0; i < headers.length; i += 2) {
            put.setHeader((String) headers[i], (String) headers[i + 1]);
        }
        HttpResponse resp = client.execute(put);
        EntityUtils.consume(resp.getEntity());
        assertEquals(expectedCode, resp.getStatusLine().getStatusCode());
        return resp;
    }
    
    public static void setCredentials(String username, String password) {
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        client.getCredentialsProvider().setCredentials(
                new AuthScope(servers.get(0).getHostname(), port), credentials);
    }
    
    public static void clearCredentials() {
        client.getCredentialsProvider().clear();
    }

    public static HttpResponse post(String uri, Object data, String contentType) throws Exception {
        return post(uri, data, contentType, HttpServletResponse.SC_OK);
    }

    public static HttpResponse post(String uri, Object data, String contentType, int expectedCode) throws Exception {
        return post(uri, data, contentType, expectedCode, new Object[0]);
    }

    public static HttpResponse post(String uri, Object data, String contentType, int expectedCode, Object... headers)
            throws Exception {
        HttpPost post = new HttpPost(uri);
        if (data instanceof String) {
            post.setEntity(new StringEntity((String) data, contentType, "UTF-8"));
        } else if (data instanceof byte[]) {
            byte[] byteData = (byte[]) data;
            ByteArrayInputStream bs = new ByteArrayInputStream(byteData);
            post.setEntity(new InputStreamEntity(bs, byteData.length));
        } else {
            throw new IllegalArgumentException("Unknown data type for POST method");
        }
        post.setHeader("Content-Type", contentType);
        if (headers.length % 2 != 0)
            throw new IllegalArgumentException("bad headers argument");
        for (int i = 0; i < headers.length; i += 2) {
            post.setHeader((String) headers[i], (String) headers[i + 1]);
        }
        HttpResponse resp = client.execute(post);
        EntityUtils.consume(resp.getEntity());
        assertEquals(expectedCode, resp.getStatusLine().getStatusCode());
        return resp;
    }

    public static HttpResponse delete(String uri) throws Exception {
        HttpDelete delete = new HttpDelete(uri);
        HttpResponse resp = client.execute(delete);
        EntityUtils.consume(resp.getEntity());
        return resp;
    }
    
    public static HttpResponse delete(String uri, int expectedCode, Object... headers) throws Exception {
        HttpDelete delete = new HttpDelete(uri);
        if (headers.length % 2 != 0)
            throw new IllegalArgumentException("bad headers argument");
        for (int i = 0; i < headers.length; i += 2) {
            delete.setHeader((String) headers[i], (String) headers[i + 1]);
        }
        HttpResponse resp = client.execute(delete);
        EntityUtils.consume(resp.getEntity());
        assertEquals(expectedCode, resp.getStatusLine().getStatusCode());
        return resp;
    }

    /**
     * returns full uri for given server number cache name and key if key is null the key part is ommited
     */
    public static String fullPathKey(int server, String cache, String key, int offset) {
        StringBuffer sb = new StringBuffer("http://" + servers.get(server).getHostname() + ":" + (port+offset)
                + servers.get(server).getRestServerPath() + "/" + cache);
        if (key == null) {
            return sb.toString();
        } else {
            return sb.append("/").append(key).toString();
        }
    }

    public static String fullPathKey(int server, String key) {
        return fullPathKey(server, "___defaultcache", key, 0);
    }

    public static String fullPathKey(int server, String key, int portOffset) {
        return fullPathKey(server, "___defaultcache", key, portOffset);
    }

    public static String fullPathKey(String key) {
        return fullPathKey(0, key);
    }

    public static String fullPathKey(String cache, String key) {
        return fullPathKey(0, cache, key, 0);
    }

    public static String fullPathKey(String cache, String key, int portOffset) {
        return fullPathKey(0, cache, key, portOffset);
    }

    public static class Server {
        private String hostname;
        private String restServerPath;

        public Server(String hostname, String restServerPath) {
            this.hostname = hostname;
            this.restServerPath = restServerPath;
        }

        public String getHostname() {
            return hostname;
        }

        public String getRestServerPath() {
            return restServerPath;
        }
    }
}