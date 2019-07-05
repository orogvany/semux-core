/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.http;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.api.ApiVersion;
import org.semux.api.SemuxApiService;
import org.semux.rules.KernelRule;
import org.semux.util.BasicAuth;

import io.netty.handler.codec.http.HttpHeaders;

public class HttpHandlerTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    private String uri = null;
    private Map<String, String> params = null;
    private HttpHeaders headers = null;

    private KernelMock kernel;
    private SemuxApiService server;

    private String ip;
    private int port;
    private String auth;

    @Before
    public void setUp() {
        kernel = kernelRule.getKernel();
        server = new SemuxApiService(kernel);

        ip = kernel.getConfig().apiListenIp();
        port = kernel.getConfig().apiListenPort();
        auth = BasicAuth.generateAuth(kernel.getConfig().apiUsername(), kernel.getConfig().apiPassword());

        new Thread(() -> server.start(ip, port, (m, u, p, h) -> {
            uri = u;
            params = p;
            headers = h;

            return Response.ok().entity("OK").build();
        })).start();

        // wait for server to boot up
        await().until(() -> server.isRunning());
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test(expected = IOException.class)
    public void testAuth() throws IOException {
        URL url = new URL("http://" + ip + ":" + port + "/getinfo");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("c", "d");
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream().write("e=f".getBytes());

        Scanner s = new Scanner(con.getInputStream());
        s.nextLine();
        s.close();
    }

    @Test
    public void testPOST() throws IOException {
        URL url = new URL("http://" + ip + ":" + port + "/test?a=b");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("c", "d");
        con.setRequestProperty("Authorization", auth);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream().write("e=f".getBytes());

        Scanner s = new Scanner(con.getInputStream());
        s.nextLine();
        s.close();

        assertEquals("/test", uri);
        assertEquals("b", params.get("a"));
        assertEquals("f", params.get("e"));
        assertEquals("d", headers.get("c"));
    }

    @Test
    public void testGET() throws IOException {
        URL url = new URL("http://" + ip + ":" + port + "/test?a=b&e=f");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("c", "d");
        con.setRequestProperty("Authorization", auth);
        Scanner s = new Scanner(con.getInputStream());
        s.nextLine();
        s.close();

        assertEquals("/test", uri);
        assertEquals("b", params.get("a"));
        assertEquals("f", params.get("e"));
        assertEquals("d", headers.get("c"));
    }

    @Test
    public void testGetStaticFiles() throws IOException {
        Map<String, String> testCases = new HashMap<>();
        testCases.put(server.getApiExplorerUrl(), "text/html");
        testCases.put(String.format("http://%s:%d/swagger-ui/swagger-ui.css", server.getIp(), server.getPort()),
                "text/css");
        testCases.put(String.format("http://%s:%d/swagger-ui/swagger-ui-bundle.js", server.getIp(), server.getPort()),
                "text/javascript");
        testCases.put(String
                .format("http://%s:%d/swagger-ui/swagger-ui-standalone-preset.js", server.getIp(), server.getPort()),
                "text/javascript");

        for (Map.Entry<String, String> e : testCases.entrySet()) {
            URL url = new URL(e.getKey());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Authorization", auth);

            StringBuilder lines = new StringBuilder();
            Scanner s = new Scanner(con.getInputStream());
            while (s.hasNextLine()) {
                lines.append(s.nextLine());
            }
            s.close();

            assertEquals(HTTP_OK, con.getResponseCode());
            assertEquals(e.getValue(), con.getHeaderField("content-type"));
            assertTrue(lines.toString().length() > 1);
        }
    }

    @Test
    public void testGetStaticFiles404() throws IOException {
        URL url = new URL("http://" + ip + ":" + port + "/" + ApiVersion.DEFAULT.prefix + "/xx.html");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", auth);

        assertEquals(HTTP_NOT_FOUND, con.getResponseCode());
    }

    @Test
    public void testKeepAlive() throws IOException {
        for (int i = 0; i < 2; i++) {
            URL url = new URL("http://" + ip + ":" + port + "/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("Authorization", auth);

            Scanner s = new Scanner(con.getInputStream());
            s.nextLine();
            s.close();

            assertEquals(HTTP_OK, con.getResponseCode());
            assertEquals("keep-alive", con.getHeaderField("connection"));
        }
    }
}
